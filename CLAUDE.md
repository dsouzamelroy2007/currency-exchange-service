# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A Spring Boot 2 REST microservice (`com.assignment.sm`, main class `BitcoinExchangeService`) that serves real-time and historical BTC-to-USD exchange rates. Java 11, built with Maven.

## Commands

```bash
# Build (clean + package)
mvn clean package

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ExchangeRateServiceTest

# Run a single test method
mvn test -Dtest=ExchangeRateServiceTest#methodName

# Run the app without Docker
mvn spring-boot:run

# Build + Docker image + run container (see start.sh)
sh start.sh
```

The app runs on `http://localhost:8080/btc` (context path `/btc`).
- Swagger UI: `http://localhost:8080/btc/swagger-ui.html`
- H2 console: `http://localhost:8080/btc/h2-console/login.jsp` (JDBC URL `jdbc:h2:mem:bitcoindb`, user/pass `bitcoin_exchange`/`bitcoin_exchange`)

Key runtime config lives in `src/main/resources/application.properties`, including `fromCurrency`/`toCurrency` (currently BTC/USD), the external exchange API URLs, and `exchangeRate.check.periodInMilliseconds` (the real-time rate poll interval).

## Architecture

Two endpoints under `/exchange` (`ExchangeRateController`), both delegating to `ExchangeRateService` (the core engine), which is backed by the static helper `ExchangeRateUtil`:

**Real-time rate (`GET /exchange/liveRate`)**
- `fetchBitcoinExchangeRate` in `ExchangeRateService` is an `@Async` `@Scheduled` (fixed-rate, no initial delay) method that polls the exchange server on the configured interval and stores the latest rate in a plain in-memory `CurrencyExchangeRate` object.
- The controller endpoint just reads that in-memory object for fast retrieval — it never calls the exchange server directly.
- Since only one currency pair is supported today, a single object suffices; supporting more pairs would require switching this to a map or a cache like Redis (noted in the original README as a known limitation, not yet implemented).

**Historical rates (`GET /exchange/historicalRate?startDate=...&endDate=...`)**
- `getHistoricalExchangeRates` first checks the H2-backed `HistoricalExchangeRateRepository` for the requested date range (via the `Currency` entity, which has a one-to-many relationship to `HistoricalExchangeRate`).
- Any missing dates are fetched from the external exchange server (`getHistoricalRatesFromServer`), which builds the request URL/limit via `ExchangeRateUtil` based on which dates are missing.
- Fetched data is merged with DB results for the response, and asynchronously persisted via `HistoricalRateCacheService` so subsequent requests for the same dates hit the DB instead of the external API.
- Protected by a Hystrix circuit breaker (`@HystrixCommand`, 120s timeout) with a fallback method (`fallback_getHistoricalExchangeRate`) returning a 502 if the exchange server call doesn't complete in time. Domain exceptions (`InvalidInputException`, `CurrencyNotFoundException`, `ExchangeRateSaveException`, `ExchangeRateFetchException`) are excluded from triggering the fallback via `ignoreExceptions`.
- A single `Currency` row (abbreviation `USD`) is seeded at startup via `DataLoader` since only one target currency is currently supported.

**External calls**: `RestService` wraps the outbound HTTP calls to the exchange APIs (`realTime.exchangeAPI`, `historical.exchangeAPI` in `application.properties`) using the `RestTemplate` bean defined in `BitcoinExchangeService` (10s connect/read timeouts).

**Error handling**: `ControllerExceptionHandler` (`@ControllerAdvice`) maps the domain exceptions in `exception/` to HTTP responses using `ErrorDetails`.

**Async infra**: `@EnableAsync` with a dedicated `threadPoolTaskExecutor` bean (core size 1, max size 100) used by the scheduled real-time fetch and the async historical-rate cache writes.

Tests under `src/test/java` mirror the `service`/`controller`/`util` packages; `MockDataCreator` and the JSON fixtures in `src/test/resources` (`bitcoinRealTimeExchangeData.json`, `historicalExchangeData.json`, `historicalExchangeBadData.json`) provide sample exchange-server responses for tests.
