package com.assignment.sm.controller;

import com.assignment.sm.exception.CurrencyNotFoundException;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.exception.HistoricalRateUnavailableException;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.ExchangeRateResponse;
import com.assignment.sm.service.ExchangeRateService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange")
@Slf4j
@Tag(name = "exchange-rate-controller", description = "REST APIs for real-time and historical currency exchange rates")
public class ExchangeRateController {

  @Autowired
  private ExchangeRateService exchangeRateService;

  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Success|OK"),
      @ApiResponse(responseCode = "400", description = "bad request!"),
      @ApiResponse(responseCode = "404", description = "not found!!!"),
      @ApiResponse(responseCode = "500", description = "Internal Server Error!!!")})
  @Operation(summary = "Get the real-time exchange rate for a currency pair")
  @RequestMapping(method = RequestMethod.GET, value = "/liveRate")
  public ResponseEntity<ExchangeRateResponse> getLatestExchangeRate(
                                          @RequestParam(required = true) String from,
                                          @RequestParam(required = true) String to
                                        ) {
    Instant startTime = Instant.now();
    try {
      CurrencyExchangeRate exchangeRate = exchangeRateService.getLiveRate(from.toUpperCase(), to.toUpperCase());
      ExchangeRateResponse response = new ExchangeRateResponse(List.of(exchangeRate));
      return new ResponseEntity(response, HttpStatus.OK);
    } finally {
      log.info("RequestType: {}, Response_Code: {}, Timestamp: {}ms", "live_exchange_rate", HttpStatus.OK,
          Duration
              .between(startTime, Instant.now())
              .toMillis());
    }
  }

  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Success|OK"),
      @ApiResponse(responseCode = "400", description = "bad request!"),
      @ApiResponse(responseCode = "404", description = "not found!!!"),
      @ApiResponse(responseCode = "500", description = "Internal Server Error!!!")})
  @Operation(summary = "Get historical exchange rates for a currency pair from Start Date (yyyy-MM-dd) to End Date (yyyy-MM-dd)")
  @RequestMapping(method = RequestMethod.GET, value = "/historicalRate")
  @CircuitBreaker(name = "historicalRate", fallbackMethod = "fallback_getHistoricalExchangeRate")
  public ResponseEntity getHistoricalExchangeRate(
                                          @RequestParam(required = true) String from,
                                          @RequestParam(required = true) String to,
                                          @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") @Valid LocalDate startDate,
                                          @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") @Valid LocalDate endDate
                                        ) {
    Instant startTime = Instant.now();
    try {
      if(startDate.isAfter(endDate)){
        throw new InvalidInputException("StartDate is greater than EndDate");
      }
      List<CurrencyExchangeRate> exchangeRates = exchangeRateService.getHistoricalExchangeRates(from.toUpperCase(), to.toUpperCase(), startDate, endDate);
      ExchangeRateResponse response = new ExchangeRateResponse(exchangeRates);
      return new ResponseEntity(response, HttpStatus.OK);
    } finally {
      log.info("RequestType: {}, Response_Code: {}, Timestamp: {}ms", "historical_exchange_rate", HttpStatus.OK,
          Duration
              .between(startTime, Instant.now())
              .toMillis());
    }
  }


  // Resilience4j dispatches to the most-specific fallback overload matching the thrown exception's
  // type. These re-throw so the domain exceptions below reach ControllerExceptionHandler as
  // themselves - resilience4j.circuitbreaker...ignoreExceptions in application.properties only
  // keeps them out of the circuit breaker's failure-rate accounting, it does NOT skip the fallback
  // dispatch (confirmed live: without these overloads, an ignored exception still lands in the
  // generic Throwable fallback below and gets mapped to a misleading 502).
  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, InvalidInputException e){
    throw e;
  }

  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, CurrencyNotFoundException e){
    throw e;
  }

  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, ExchangeRateSaveException e){
    throw e;
  }

  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, ExchangeRateFetchException e){
    throw e;
  }

  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, HistoricalRateUnavailableException e){
    throw e;
  }

  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate, Throwable t){
    return new ResponseEntity("Request made to the Currency Exchange server for historical dates has timed out. Please try again after sometime.", HttpStatus.BAD_GATEWAY);
  }
}
