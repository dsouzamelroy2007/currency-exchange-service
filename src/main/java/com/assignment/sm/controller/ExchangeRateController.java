package com.assignment.sm.controller;

import com.assignment.sm.exception.CurrencyNotFoundException;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.ExchangeRateResponse;
import com.assignment.sm.service.ExchangeRateService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
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
@Api(value = "REST APIs for real-time and historical currency exchange rates" )
public class ExchangeRateController {

  @Autowired
  private ExchangeRateService exchangeRateService;

  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Success|OK"),
      @ApiResponse(code = 400, message = "bad request!"),
      @ApiResponse(code = 404, message = "not found!!!"),
      @ApiResponse(code = 500, message = "Internal Server Error!!!")})
  @ApiOperation(value = "Get the real-time exchange rate for a currency pair", tags = "getLatestExchangeRate")
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
      @ApiResponse(code = 200, message = "Success|OK"),
      @ApiResponse(code = 400, message = "bad request!"),
      @ApiResponse(code = 404, message = "not found!!!"),
      @ApiResponse(code = 500, message = "Internal Server Error!!!")})
  @ApiOperation(value = "Get historical exchange rates for a currency pair from Start Date (yyyy-MM-dd) to End Date (yyyy-MM-dd) ", response = ExchangeRateResponse.class, tags = "getHistoricalExchangeRate")
  @RequestMapping(method = RequestMethod.GET, value = "/historicalRate")
  @HystrixCommand(fallbackMethod = "fallback_getHistoricalExchangeRate",
                  commandProperties = {
                                         @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "120000")
                                       },
                  ignoreExceptions = {InvalidInputException.class, CurrencyNotFoundException.class, ExchangeRateSaveException.class,
                                      ExchangeRateSaveException.class, ExchangeRateFetchException.class}
                  )
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


  public ResponseEntity fallback_getHistoricalExchangeRate(String from, String to, LocalDate startDate, LocalDate endDate){
    return new ResponseEntity("Request made to the Currency Exchange server for historical dates has timed out. Please try again after sometime.", HttpStatus.BAD_GATEWAY);
  }
}
