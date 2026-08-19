# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Two parts:
- **Backend** (repo root): a Spring Boot 2 REST + WebSocket microservice (`com.assignment.sm`,
  main class `BitcoinExchangeService`) that serves real-time and historical exchange rates for
  *any* currency pair (not just BTC/USD despite the class name — pairs are created on demand).
  Java 11, Maven.
- **Frontend** (`frontend/`): a React + Vite + TypeScript SPA, an independent npm project not
  wired into the Maven build. Consumes the backend's REST endpoints and its STOMP/SockJS live-rate
  push.

`plan.md` has the frontend's original stage-by-stage build plan. `README.md` and
`frontend/README.md` each have a "Decisions" section explaining *why* behind the choices below —
this file is about *how it currently works and where things live*.

## Commands

**Backend** (repo root):
```bash
mvn clean package              # build (clean + package)
mvn test                       # run tests
mvn test -Dtest=ExchangeRateServiceTest              # single test class
mvn test -Dtest=ExchangeRateServiceTest#methodName   # single test method
mvn spring-boot:run            # run without Docker
sh start.sh                    # build + Docker image + run container
```
Runs on `http://localhost:8080/btc` (context path `/btc`).
- Swagger UI: `http://localhost:8080/btc/swagger-ui.html`
- H2 console: `http://localhost:8080/btc/h2-console/login.jsp` — JDBC URL
  `jdbc:h2:file:./data/bitcoindb;AUTO_SERVER=TRUE`, user/pass `bitcoin_exchange`/`bitcoin_exchange`
  (file-based, not in-memory — data in `./data/` survives restarts; schema is Flyway-managed,
  `src/main/resources/db/migration/`).

**Frontend** (`frontend/`):
```bash
npm install
npm run dev      # dev server on :5173, proxying /btc to the backend on :8080
npm run build    # typecheck (tsc -b) + production build
npm run test     # Vitest + React Testing Library
```
Run the backend alongside `npm run dev` for a working local setup — see "Local gotchas" below,
there are a couple of non-obvious ones.

Key runtime config lives in `src/main/resources/application.properties`: the two rate-provider
base URLs, cache TTLs, `exchangeRate.check.periodInMilliseconds` (live-rate poll interval),
`websocket.maxActivePairs`, and `cors.allowedOrigins`.

## Backend architecture

Two endpoints under `/exchange` (`ExchangeRateController`), both delegating to
`ExchangeRateService` (the core engine), backed by the static helper `ExchangeRateUtil` and
`CurrencyPairService` (creates `Currency`/`CurrencyPair` rows on first use of a pair — no seeding,
no fixed pair list).

**Live rate (`GET /exchange/liveRate?from=&to=`)**
- `ExchangeRateService.fetchExchangeRates(from, toCurrencies)` tries the free provider first
  (`ExchangeRateApiProviderService`, wraps `https://open.er-api.com/v6/latest/{base}` — no API key,
  but **fiat-only**, one call covers every target currency for that base, cached per base currency
  for 24h via the `externalBaseRates` Caffeine cache). Falls back to `realTime.exchangeAPI`
  (CryptoCompare, needs a key it doesn't have right now) when the free provider doesn't cover the
  base (e.g. crypto) or is unreachable.
- `getLiveRate` (called by the REST endpoint) is itself `@Cacheable(value = "liveRates")`, TTL
  `cache.liveRates.ttlSeconds`.
- Separately, `refreshSubscribedLiveRates` is `@Scheduled` (`exchangeRate.check.periodInMilliseconds`)
  and polls only *actively WS-subscribed* pairs (`RateSubscriptionRegistry.getActivePairs()`),
  pushing updates via STOMP. `onNewRateSubscription` (an `@EventListener`) fires an immediate push
  the moment a pair gets its first subscriber, so a new subscriber doesn't wait for the next tick.
- Every push (`publishLiveRate`) also calls `HistoricalRateCacheService.recordTodaysRateIfMissing`
  — see "Historical rate" below.

**Historical rate (`GET /exchange/historicalRate?from=&to=&startDate=&endDate=`)**
- DB-first: checks `HistoricalExchangeRateRepository` for the requested range. If fully covered,
  returns straight from the DB.
- Missing dates are fetched from CryptoCompare (`getHistoricalRatesFromServer`, using
  `ExchangeRateUtil` to build the URL/limit) — **there is no free provider for historical data**;
  both CryptoCompare and exchangerate-api.com require a paid plan for it, so this call currently
  always fails without a key.
- **Because of that**, there's a second, independent way historical data gets populated: every live
  push (`publishLiveRate`, both the scheduled poll and the immediate on-subscribe push) calls
  `HistoricalRateCacheService.recordTodaysRateIfMissing`, which writes one row per pair per day
  (guarded by `existsByCurrencyPairAndDate` so the ~10s poll doesn't hammer the DB). So any pair
  tracked live in the frontend accumulates real historical data going forward — there's nothing for
  dates before a pair was first tracked, and that's inherent to not having a real historical
  source, not a bug.
- If the external fetch fails (the normal case right now) and the DB doesn't fully cover the
  range, `getHistoricalRatesFromServer` throws `HistoricalRateUnavailableException` (404) with a
  message naming the pair, the range, how many days *are* covered locally, and that data
  accumulates automatically once a pair is tracked live — instead of the raw upstream exception.
- Protected by `@HystrixCommand` (120s timeout) with `fallback_getHistoricalExchangeRate` (returns
  502 plain text on a genuine timeout). Domain exceptions — `InvalidInputException`,
  `CurrencyNotFoundException`, `ExchangeRateSaveException`, `ExchangeRateFetchException`,
  `HistoricalRateUnavailableException` — are in `ignoreExceptions` so they reach the client as
  themselves instead of being replaced by the Hystrix fallback text.

**WebSocket** (`WebSocketConfig`): STOMP over SockJS at `/ws` (full path `/btc/ws`). Clients
`SUBSCRIBE /topic/rates/{FROM}/{TO}` (uppercase). `RateSubscriptionRegistry` (a `ChannelInterceptor`)
tracks which pairs have at least one subscriber, enforces `websocket.maxActivePairs`, and publishes
`NewRateSubscriptionEvent` on a pair's first subscriber. Exceeding the cap throws inside the
interceptor → STOMP `ERROR` frame → the whole session drops (all of that connection's subscriptions).

**Caching**: two independent Caffeine caches, both registered in `CacheConfig` via
`registerCustomCache` (different TTLs from `application.properties`):
- `liveRates` — per-pair (`FROM_TO` key), short TTL (`cache.liveRates.ttlSeconds`).
- `externalBaseRates` — per-base-currency, 24h TTL (`externalRatesApi.cache.ttlSeconds`). Provider
  failures aren't cached (`unless = "#result == null"` on the `@Cacheable`), so a transient outage
  doesn't lock out the fallback path for a full day.

**External calls**: `RestService.get(url, Class)` wraps outbound HTTP via the `RestTemplate` bean
(10s connect/read timeouts, defined in `BitcoinExchangeService`), always throwing
`ExchangeRateFetchException` on any failure (non-2xx or network error) — this is the type callers
should catch to distinguish "the external call failed" from other exceptions.

**Error handling**: `ControllerExceptionHandler` (`@ControllerAdvice`) maps the domain exceptions
in `exception/` to HTTP responses using `ErrorDetails`.

**Async infra**: `@EnableAsync` with `threadPoolTaskExecutor` (core size 1, max size 100), used by
the scheduled live-rate refresh, `onNewRateSubscription`, and the async historical-rate DB writes.

**Timezone**: `TimeZone.setDefault(UTC)` runs as the *first line of `main()`*, before
`SpringApplication.run(...)` — not in `@PostConstruct`. It used to be in `@PostConstruct`, which
runs after Spring creates the DataSource/connection pool; H2's JDBC driver falls back to
`Calendar.getInstance()`/`TimeZone.getDefault()` for date conversions, so connections created
before the override landed stayed pinned to the JVM's original zone, causing `HistoricalExchangeRate.date`
to read back one day off from what was actually stored whenever the host machine's timezone wasn't
UTC. If you ever see a historical date off by one, check this hasn't regressed.

Tests under `src/test/java` mirror `service`/`controller`/`util`/`websocket`; `MockDataCreator` and
the JSON fixtures in `src/test/resources` provide sample exchange-server responses.
**Known pre-existing flaky test**: `ExchangeRateServiceTest#testGetHistoricalExchangeRatesFromExchangeServerSuccess`
hardcodes an epoch timestamp that only matches a specific system timezone — fails on machines
where that doesn't hold (confirmed failing on `main` independent of any other change, via
`git stash`). Not something introduced by recent work; hasn't been fixed since it's outside the
scope of whatever else was being done at the time.

## Frontend architecture

```
frontend/src/
  main.tsx, App.tsx              # entry; App renders Dashboard + HistoricalRateChart
  styles/global.css              # CSS custom properties (light + prefers-color-scheme: dark)
  api/types.ts, httpClient.ts, exchangeRateClient.ts   # REST client
  ws/stompConnectionManager.ts, types.ts               # STOMP/SockJS singleton
  currencies/knownCurrencies.ts  # static list: code, name, symbol + filterCurrencies()
  hooks/useLiveRate.ts, useHistoricalRate.ts, useStompConnectionStatus.ts, useTrackedPairs.ts
  components/
    pair-picker/CurrencyPairPicker.tsx, CurrencyCombobox.tsx
    dashboard/Dashboard.tsx, PairCard.tsx, AddPairForm.tsx
    chart/HistoricalRateChart.tsx, DateRangePicker.tsx
    common/ConnectionStatusBadge.tsx
  utils/currencyPair.ts (pairKey, normalizePair, pairAccentColor), dateRange.ts
frontend/tests/   # Vitest + RTL, mirrors src/ for the 3 files that have tests
```

**REST** (`api/`): `httpClient.ts` tries `res.json()` then falls back to `res.text()` on error
bodies, since the Hystrix-timeout 502 is plain text but everything else is JSON `ErrorDetails`.
`exchangeRateClient.ts` uppercases codes client-side and unwraps `.exchangeRates`.

**WebSocket** (`ws/stompConnectionManager.ts`): module-level singleton. `subscribe(from, to, onMessage)`
ref-counts listeners per `pairKey` so multiple components sharing a pair share one STOMP
subscription; only sends STOMP `UNSUBSCRIBE` when the last local listener drops. Lazy-connects on
first `subscribe()` call. On `onStompError` (cap exceeded / session drop): clears local subscription
bookkeeping and relies on `@stomp/stompjs`'s `reconnectDelay` for retry cadence, re-issuing
`SUBSCRIBE` for every still-wanted pair on reconnect — no user-facing error, just the
`ConnectionStatusBadge` (connecting/open/reconnecting).

**State**: `useTrackedPairs` persists the dashboard's pair list to `localStorage`, seeded with
`BTC/USD`. `useLiveRate(from, to)` does an initial REST fetch then subscribes for live updates.
`useHistoricalRate` fetches only on explicit "Apply" (not per-keystroke — the 120s Hystrix ceiling
on a bad range makes that expensive).

**Currency symbols & color** (see `frontend/README.md` Decisions for the "why"):
`knownCurrencies.ts` has a `symbol` field per entry; `currencySymbol(code)` falls back to the
uppercased code for anything not in the static list. `pairAccentColor` (in `utils/currencyPair.ts`)
hashes the pair key into an HSL hue — deterministic, no state — exposed as the `--pair-accent` CSS
custom property on `.pair-card`.

## Local gotchas

- **`sockjs-client` references Node's `global`** — not polyfilled by Vite's browser build by
  default. `vite.config.ts` has `define: { global: 'globalThis' }`; without it the app crashes on
  load with `ReferenceError: global is not defined`.
- **`tsc -b` (used by `npm run build`) will happily compile `vite.config.ts` into a stale
  `vite.config.js`** sitting right next to it, which Vite then silently prefers over the `.ts` file
  — meaning edits to `vite.config.ts` (e.g. changing the proxy target) get ignored with zero error.
  `tsconfig.node.json` sets `"outDir": "./node_modules/.tsbuild"` to keep emitted JS out of the way.
  If a proxy-target change doesn't seem to take effect, check for a stray `vite.config.js` first.
- **Free live-rate provider is fiat-only.** Crypto pairs (`BTC/USD`, etc.) fall back to
  CryptoCompare, which needs a key that isn't configured — those pairs will sit on "Waiting for
  rate…" indefinitely without one. Fiat pairs (`USD/EUR`, `USD/GBP`, ...) work with no key.
- **Ports 8080/5173 may already be in use** by unrelated local projects — check before assuming a
  bind failure means *this* app is broken, and don't blanket-`pkill` by process name (it can catch
  another project's dev server too).
