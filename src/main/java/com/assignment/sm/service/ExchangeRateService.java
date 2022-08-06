package com.assignment.sm.service;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.HistoricalRateURLInfo;
import com.assignment.sm.repository.CurrencyRepository;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.ExchangeRateUtil;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@Slf4j
@Setter
public class ExchangeRateService {


  @Value("${toCurrency}")
  private String targetCurrency;

  //@Value("#{'${targetCurrencies}'.split(',')}")
  //private List<String> targetCurrencies;

  @Value("${realTime.exchangeAPI}")
  private String exchangeAPI;

  @Value("${historical.exchangeAPI}")
  private String historicalExchangeRateServerBaseURL;

  @Value("${fromCurrency}")
  private String fromCurrency;

  @Autowired
  RestService restService;

  @Autowired
  CurrencyRepository currencyRepository;

  @Autowired
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  @Autowired
  HistoricalRateCacheService historicalRateCacheService;

  private CurrencyExchangeRate currencyExchangeRate;

  public CurrencyExchangeRate getCurrencyExchangeRate(){
    if(currencyExchangeRate != null){
      return currencyExchangeRate;
    }else{
      return new CurrencyExchangeRate(targetCurrency);
    }
  }

  public List<CurrencyExchangeRate> getHistoricalExchangeRates(String targetCurrency, LocalDate startDate, LocalDate endDate){
    try{
      Currency currency = currencyRepository.findByAbbreviation(targetCurrency);
      List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRateRepository.findByCurrencyAndDateBetweenOrderByDateAsc(currency, startDate, endDate);
      int numberOfMissingDatesFromLocalStorage = ExchangeRateUtil.findMissingDays(historicalExchangeRates.size(), startDate, endDate);
      if(numberOfMissingDatesFromLocalStorage == 0){
        return ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate, targetCurrency, historicalExchangeRates, null);
      }
      log.info("Data not availabe in local storage for all the historical dates");
      return getHistoricalRatesFromServer(currency, historicalExchangeRates, startDate, endDate);
    }catch (Exception e){
      log.error("Error while fetching historical exchange rates from startDate : {} to endDate : {}", startDate, endDate,e);
      throw new ExchangeRateFetchException(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause());
    }
  }

  public  List<CurrencyExchangeRate> getHistoricalRatesFromServer(Currency currency, List<HistoricalExchangeRate> localHistoricalExchangeRates, LocalDate startDate, LocalDate endDate){
    HistoricalRateURLInfo URLInfo = ExchangeRateUtil.getInfoOnHistoricalRatesToBeFetched(localHistoricalExchangeRates, startDate, endDate);
    String historicalExchangeServerAPIURL;
    List<Map<String, Object>> bitcoinHistoricalExhangeRates = new ArrayList<>();
    if(URLInfo.getLowerLimitTimeStamp() != null && URLInfo.getUpperLimitTimeStamp() != null){
      historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, targetCurrency, URLInfo.getLowerLimit(), URLInfo.getLowerLimitTimeStamp());
      bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
      historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, targetCurrency, URLInfo.getUpperLimit(), URLInfo.getUpperLimitTimeStamp());
      bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
    }else if(URLInfo.getLowerLimitTimeStamp() != null){
      historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, targetCurrency, URLInfo.getLowerLimit() , URLInfo.getLowerLimitTimeStamp());
      bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
    }else{
      historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, targetCurrency, URLInfo.getUpperLimit() , URLInfo.getUpperLimitTimeStamp());
      bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
    }

    historicalRateCacheService.saveMissingHistoricalExchangeRates(startDate, localHistoricalExchangeRates, currency, bitcoinHistoricalExhangeRates);
    return ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate, currency.getAbbreviation(), localHistoricalExchangeRates, bitcoinHistoricalExhangeRates);
  }

  @Scheduled(fixedRateString = "${exchangeRate.check.periodInMilliseconds}")
  public void fetchBitCoinExchangeRate(){
    try{
      Map<String, Object> bitcoinExhangeRates = restService.get(this.exchangeAPI, Map.class);
      CurrencyExchangeRate currencyExchangeRate = ExchangeRateUtil.getCurrencyExchangeRate(this.targetCurrency, bitcoinExhangeRates, this.getCurrencyExchangeRate());
      this.currencyExchangeRate = currencyExchangeRate;
    }catch (Exception e){
      log.error("Error while fetching realTime exchange rate",e);
      throw new ExchangeRateFetchException(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause());
    }
  }

}
