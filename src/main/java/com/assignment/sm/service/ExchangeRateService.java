package com.assignment.sm.service;

import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.exception.HistoricalRateUnavailableException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.HistoricalRateURLInfo;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.ExchangeRateUtil;
import com.assignment.sm.websocket.NewRateSubscriptionEvent;
import com.assignment.sm.websocket.RateSubscriptionRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@Slf4j
@Setter
public class ExchangeRateService {

  @Value("${realTime.exchangeAPI}")
  private String exchangeAPI;

  @Value("${historical.exchangeAPI}")
  private String historicalExchangeRateServerBaseURL;

  @Autowired
  RestService restService;

  @Autowired
  ExchangeRateApiProviderService exchangeRateApiProviderService;

  @Autowired
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  @Autowired
  HistoricalRateCacheService historicalRateCacheService;

  @Autowired
  CurrencyPairService currencyPairService;

  @Autowired
  RateSubscriptionRegistry rateSubscriptionRegistry;

  @Autowired
  SimpMessagingTemplate messagingTemplate;

  @Autowired
  CacheManager cacheManager;

  @Cacheable(value = "liveRates", key = "#from + '_' + #to")
  public CurrencyExchangeRate getLiveRate(String from, String to){
    currencyPairService.findOrCreatePair(from, to);
    Map<String, Object> exchangeRates = fetchExchangeRates(from, List.of(to));
    return ExchangeRateUtil.getCurrencyExchangeRate(from, to, exchangeRates, new CurrencyExchangeRate());
  }

  private Map<String, Object> fetchExchangeRates(String from, List<String> toCurrencies){
    Map<String, Object> freeProviderRates = exchangeRateApiProviderService.getRatesForBase(from);
    if(freeProviderRates != null && toCurrencies.stream().allMatch(freeProviderRates::containsKey)){
      return freeProviderRates;
    }
    return restService.get(ExchangeRateUtil.getURLStringToFetchRealTimeExchangeRate(exchangeAPI, from, toCurrencies), Map.class);
  }

  public List<CurrencyExchangeRate> getHistoricalExchangeRates(String from, String to, LocalDate startDate, LocalDate endDate){
    CurrencyPair currencyPair = currencyPairService.findOrCreatePair(from, to);
    try{
      List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRateRepository.findByCurrencyPairAndDateBetweenOrderByDateAsc(currencyPair, startDate, endDate);
      int numberOfMissingDatesFromLocalStorage = ExchangeRateUtil.findMissingDays(historicalExchangeRates.size(), startDate, endDate);
      if(numberOfMissingDatesFromLocalStorage == 0){
        return ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate, currencyPair, historicalExchangeRates, null);
      }
      log.info("Data not availabe in local storage for all the historical dates");
      return getHistoricalRatesFromServer(currencyPair, historicalExchangeRates, startDate, endDate);
    }catch (HistoricalRateUnavailableException e){
      throw e;
    }catch (Exception e){
      log.error("Error while fetching historical exchange rates from startDate : {} to endDate : {}", startDate, endDate,e);
      throw new ExchangeRateFetchException(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
  }

  public List<CurrencyExchangeRate> getHistoricalRatesFromServer(CurrencyPair currencyPair, List<HistoricalExchangeRate> localHistoricalExchangeRates, LocalDate startDate, LocalDate endDate){
    HistoricalRateURLInfo URLInfo = ExchangeRateUtil.getInfoOnHistoricalRatesToBeFetched(localHistoricalExchangeRates, startDate, endDate);
    String fromCurrency = currencyPair.getFromCurrency().getAbbreviation();
    String toCurrency = currencyPair.getToCurrency().getAbbreviation();
    String historicalExchangeServerAPIURL;
    List<Map<String, Object>> bitcoinHistoricalExhangeRates = new ArrayList<>();
    try{
      if(URLInfo.getLowerLimitTimeStamp() != null && URLInfo.getUpperLimitTimeStamp() != null){
        historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, toCurrency, URLInfo.getLowerLimit(), URLInfo.getLowerLimitTimeStamp());
        bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
        historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, toCurrency, URLInfo.getUpperLimit(), URLInfo.getUpperLimitTimeStamp());
        bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
      }else if(URLInfo.getLowerLimitTimeStamp() != null){
        historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, toCurrency, URLInfo.getLowerLimit() , URLInfo.getLowerLimitTimeStamp());
        bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
      }else{
        historicalExchangeServerAPIURL = ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates(historicalExchangeRateServerBaseURL, fromCurrency, toCurrency, URLInfo.getUpperLimit() , URLInfo.getUpperLimitTimeStamp());
        bitcoinHistoricalExhangeRates.add(restService.get(historicalExchangeServerAPIURL, Map.class));
      }
    }catch (ExchangeRateFetchException e){
      int availableDays = localHistoricalExchangeRates.size();
      log.warn("External historical rate provider unavailable for {}/{} between {} and {}; {} of the requested days were already tracked locally",
          fromCurrency, toCurrency, startDate, endDate, availableDays, e);
      throw new HistoricalRateUnavailableException(HttpStatus.NOT_FOUND, String.format(
          "No historical rate data available for %s/%s for the full range %s to %s (only %d day(s) previously tracked locally). "
              + "This pair hasn't been searched historically before, and the external historical rate provider isn't reachable to backfill the rest. "
              + "Rates for %s/%s are recorded automatically going forward once the pair is tracked live.",
          fromCurrency, toCurrency, startDate, endDate, availableDays, fromCurrency, toCurrency));
    }

    historicalRateCacheService.saveMissingHistoricalExchangeRates(startDate, localHistoricalExchangeRates, currencyPair, bitcoinHistoricalExhangeRates);
    return ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate, currencyPair, localHistoricalExchangeRates, bitcoinHistoricalExhangeRates);
  }

  @Scheduled(fixedRateString = "${exchangeRate.check.periodInMilliseconds}")
  public void refreshSubscribedLiveRates(){
    Set<String> activePairs = rateSubscriptionRegistry.getActivePairs();
    if(activePairs.isEmpty()){
      return;
    }
    Map<String, List<String>> toCurrenciesByFrom = activePairs.stream()
        .map(pairKey -> pairKey.split("_", 2))
        .collect(Collectors.groupingBy(parts -> parts[0], Collectors.mapping(parts -> parts[1], Collectors.toList())));

    toCurrenciesByFrom.forEach(this::refreshAndPushRatesForBase);
  }

  @EventListener
  @Async("threadPoolTaskExecutor")
  public void onNewRateSubscription(NewRateSubscriptionEvent event){
    refreshAndPushRatesForBase(event.getFromCurrency(), List.of(event.getToCurrency()));
  }

  private void refreshAndPushRatesForBase(String from, List<String> toCurrencies){
    try{
      Map<String, Object> exchangeRates = fetchExchangeRates(from, toCurrencies);
      toCurrencies.forEach(to -> publishLiveRate(from, to, exchangeRates));
    }catch (Exception e){
      log.error("Error while refreshing subscribed live rates for base currency {}", from, e);
    }
  }

  private void publishLiveRate(String from, String to, Map<String, Object> exchangeRates){
    try{
      CurrencyExchangeRate rate = ExchangeRateUtil.getCurrencyExchangeRate(from, to, exchangeRates, new CurrencyExchangeRate());
      cacheManager.getCache("liveRates").put(RateSubscriptionRegistry.pairKey(from, to), rate);
      messagingTemplate.convertAndSend("/topic/rates/" + from + "/" + to, rate);
      CurrencyPair currencyPair = currencyPairService.findOrCreatePair(from, to);
      historicalRateCacheService.recordTodaysRateIfMissing(currencyPair, rate);
    }catch (Exception e){
      log.error("Error while publishing live rate for {} -> {}", from, to, e);
    }
  }

}
