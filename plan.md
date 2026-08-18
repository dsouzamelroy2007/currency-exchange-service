# Frontend: React + Vite SPA for currency-exchange-service

## Context

The backend revamp (dynamic currency pairs, REST `liveRate`/`historicalRate`, STOMP/SockJS
live-rate push) is already implemented, tested, and pushed to `upgrade`. This plan covers the
promised follow-up: a React + Vite frontend, in its own `frontend/` directory, as an independent
npm project (not wired into the Maven build — that's explicitly future work). Confirmed v1 scope:
a live rate ticker, a historical rate chart, a currency pair picker/search, and a dashboard
tracking multiple pairs at once.

## Confirmed backend contract (already live, just consumed here)

- `GET /btc/exchange/liveRate?from=&to=` → `{ exchangeRates: [{ fromCurrency, toCurrency, exchangeRate, date }] }` (1 element). 400 on `from==to`.
- `GET /btc/exchange/historicalRate?from=&to=&startDate=&endDate=` → same shape, date-ascending list. 502 plain-text body on Hystrix timeout.
- STOMP over SockJS at `/btc/ws`; clients `SUBSCRIBE /topic/rates/{FROM}/{TO}` (uppercase) to receive push updates (payload = one `CurrencyExchangeRate`). Exceeding `websocket.maxActivePairs` throws a `MessagingException` server-side → STOMP `ERROR` frame → the whole STOMP session drops (all of that tab's subscriptions).
- New pair's first subscriber gets an immediate async push in addition to the regular scheduled poll.

## Decisions locked in

- **Stack**: Vite + React + TypeScript (shared REST/WS DTOs and the subscribe contract are worth typing).
- **WS client**: `@stomp/stompjs` + `sockjs-client` — SockJS is required, not optional, since the backend endpoint is `withSockJS()`, not a raw WS upgrade.
- **Charting**: `recharts` (declarative, right-sized for one line series; `lightweight-charts` would be overkill/imperative for this).
- **Styling**: plain CSS — no design-system library for this scope.
- **Dev connectivity**: Vite proxy (`/btc` → `localhost:8080`, `ws: true`) as the primary mechanism, **plus** widening the backend's `cors.allowedOrigins` to include `http://localhost:5173` (one line in `application.properties`) so direct/non-proxied access also works.
- **Cap-error UX**: silent auto-retry. `@stomp/stompjs`'s built-in `reconnectDelay` (e.g. 5s) is the natural rate limiter — on `onStompError`, the manager treats the session as dropped, lets stompjs reconnect on its own delay, and re-subscribes all still-wanted pairs on reconnect. No blocking banner; the existing `ConnectionStatusBadge` (connecting/open/reconnecting) is the only user-visible signal. Repeated failures are logged to the console, not surfaced as an interruptive error.
- **Historical chart**: a standalone section on the page with its own pair picker + date range, decoupled from the live dashboard's tracked pairs.
- **Persistence**: tracked pairs list in `localStorage`, seeded with `BTC/USD` on first run. No cross-tab sync.
- **Currency picker**: a static, hand-maintained list (~20-30 common fiat + crypto codes) for search/autocomplete, but free-typed codes are still accepted as-is (backend creates any pair on demand).

## Directory layout (new, under `frontend/`)

```
frontend/
  package.json, tsconfig.json, tsconfig.node.json, vite.config.ts, index.html, .env.development
  src/
    main.tsx, App.tsx
    styles/global.css
    api/types.ts, httpClient.ts, exchangeRateClient.ts
    ws/stompConnectionManager.ts, types.ts
    currencies/knownCurrencies.ts
    hooks/useLiveRate.ts, useHistoricalRate.ts, useStompConnectionStatus.ts, useTrackedPairs.ts
    components/
      pair-picker/CurrencyPairPicker.tsx, CurrencyCombobox.tsx
      dashboard/Dashboard.tsx, PairCard.tsx, AddPairForm.tsx
      chart/HistoricalRateChart.tsx, DateRangePicker.tsx
      common/ConnectionStatusBadge.tsx
    utils/currencyPair.ts, dateRange.ts
  tests/setupTests.ts, currencies/knownCurrencies.filter.test.ts,
        ws/stompConnectionManager.test.ts, components/CurrencyCombobox.test.tsx
  vitest.config.ts
```

## Stage 1 — Scaffold + dev connectivity

- Author `frontend/package.json`/`vite.config.ts`/`tsconfig.json` directly (or `npm create vite@latest frontend -- --template react-ts` then trim) rather than accepting unwanted template defaults.
- Deps: `react`, `react-dom`; dev: `vite`, `@vitejs/plugin-react`, `typescript`, `@types/react`, `@types/react-dom`; `@stomp/stompjs`, `sockjs-client`, `@types/sockjs-client`; `recharts`.
- `vite.config.ts`: `server.proxy['/btc'] = { target: 'http://localhost:8080', changeOrigin: true, ws: true }`.
- `src/main/resources/application.properties` (backend, one line): `cors.allowedOrigins = http://localhost:3000,http://localhost:5173`.
- `.env.development`: `VITE_API_BASE=/btc` (relative — proxy handles routing).

## Stage 2 — API/WS client layer

- `api/types.ts`: `CurrencyExchangeRate`, `ExchangeRateResponse`, `ErrorDetails`, `ApiError` (`http` / `gateway-timeout` / `network` variants — the 502 Hystrix fallback is plain text, not JSON, so `httpClient.ts` must try `res.json()` then fall back to `res.text()`).
- `api/httpClient.ts` + `api/exchangeRateClient.ts`: `getLiveRate(from, to)`, `getHistoricalRate(from, to, startDate, endDate)`, uppercasing inputs client-side and unwrapping `.exchangeRates`.
- `ws/stompConnectionManager.ts`: module-level singleton exposing `subscribe(from, to, onMessage): () => void` and `onStatusChange(listener)`. Ref-counts subscribers per `pairKey` (`FROM_TO`, matching the backend's key) so multiple components subscribing to the same pair share one STOMP subscription; only sends STOMP `UNSUBSCRIBE` when the last local listener drops. Lazy-connects on first `subscribe()` call. On `onStompError` (cap exceeded / session drop): clears local subscription bookkeeping, relies on stompjs `reconnectDelay` for the retry cadence, and re-issues `SUBSCRIBE` for every pair still wanted once reconnected — no user-facing error surfaced (per the silent-retry decision).

## Stage 3 — Currency pair picker + multi-pair dashboard

- `currencies/knownCurrencies.ts`: `KNOWN_CURRENCIES` static list + pure `filterCurrencies(query, list)` (case-insensitive match on code or name) — this pure function is what gets unit-tested.
- `CurrencyCombobox.tsx`: text input + filtered dropdown; does not force selection from the list — an unmatched typed value is still accepted (uppercased) on blur/submit.
- `CurrencyPairPicker.tsx`: two comboboxes (from/to) + submit; validates `from !== to` client-side before calling back, mirroring the backend's rule instead of round-tripping to discover it.
- `Dashboard.tsx`: `AddPairForm` (wraps the picker) + list of `PairCard`s from `useTrackedPairs()` + one shared `ConnectionStatusBadge`.
- `PairCard.tsx`: `useLiveRate(from, to)` → current rate, last-updated date, remove button.
- `useLiveRate.ts`: on mount/pair-change, calls `getLiveRate` for an immediate value, then `stompConnectionManager.subscribe(from, to, setRate)` for live updates; unsubscribes on unmount.
- `useTrackedPairs.ts`: `localStorage`-backed `{ pairs, addPair, removePair }`, seeded with `BTC/USD` when empty, guarded against malformed stored JSON.

## Stage 4 — Historical rate chart (standalone section)

- `DateRangePicker.tsx`: two native `<input type="date">`, applied on blur/explicit action (not per-keystroke) given the 120s Hystrix ceiling on a bad range.
- `useHistoricalRate.ts`: fetches on explicit apply of `from`/`to`/date range; surfaces `ApiError` (including the `gateway-timeout` 502 case).
- `HistoricalRateChart.tsx`: `recharts` `<LineChart>` with `dataKey="exchangeRate"`, `<XAxis dataKey="date">`, `<Tooltip>`. Owns its own `from`/`to` pair picker (reusing `CurrencyPairPicker`), independent of the dashboard.

## Stage 5 — Tests (Vitest + React Testing Library)

Deps: `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom`, `@testing-library/user-event`. Three focused files, not exhaustive coverage:
1. `knownCurrencies.filter.test.ts` — `filterCurrencies` case-insensitivity, code vs. name matching, empty/no-match behavior.
2. `stompConnectionManager.test.ts` (mocked `@stomp/stompjs` client) — lazy-connect-once, shared subscription + ref-counting for duplicate pair subscribes, `unsubscribe` only sends STOMP UNSUBSCRIBE on the last listener, `onStompError` doesn't throw and clears state for reconnect.
3. `CurrencyCombobox.test.tsx` — typing filters the list, selecting sets the value, an unlisted typed value is still accepted.

## Explicitly out of scope

Wiring `frontend/dist` into the Maven build/single-jar deploy, authentication, a non-dev-proxy production hosting story, cross-tab localStorage sync, any live/third-party currency catalog, accessibility/i18n polish beyond semantic HTML.

## Verification

- `npm install && npm run dev` in `frontend/` with the Spring Boot app running (`mvn spring-boot:run` in the repo root) — confirm the dashboard loads, `BTC/USD` appears by default, adding a second pair (e.g. `ETH/EUR`) works via the picker (including a free-typed code not in the static list), and the connection badge reflects WS status. (Live external rate values won't populate in this sandbox since outbound calls to blockchain/CryptoCompare are network-policy-blocked here — verify via mocked/log-level checks or note this limitation the same way the backend verification did.)
- `npm run build` — production build succeeds with no TypeScript errors.
- `npm run test` — all three test files pass.
- Manually exercise the historical chart section with a valid date range against the (currently network-blocked-in-sandbox) `/historicalRate` endpoint, confirming loading/error states render sensibly even when the upstream call fails.
