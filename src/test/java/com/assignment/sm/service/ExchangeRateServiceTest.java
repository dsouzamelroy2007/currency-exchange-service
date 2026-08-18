package com.assignment.sm.service;

import static com.assignment.sm.util.MockDataCreator.getData;
import static com.assignment.sm.util.MockDataCreator.historicalExchangeRatesForTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.MockDataCreator;
import com.assignment.sm.websocket.NewRateSubscriptionEvent;
import com.assignment.sm.websocket.RateSubscriptionRegistry;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.QueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

  private final String exchangeAPI = "http://xyz.com/liveRates";

  private final String historicalExchangeRateServerBaseURL = "http://xyz.com/historicalRates";

  @InjectMocks
  ExchangeRateService exchangeRateService;

  @Mock
  RestService restService;

  @Mock
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  @Mock
  HistoricalRateCacheService historicalRateCacheService;

  @Mock
  CurrencyPairService currencyPairService;

  @Mock
  RateSubscriptionRegistry rateSubscriptionRegistry;

  @Mock
  SimpMessagingTemplate messagingTemplate;

  @Mock
  CacheManager cacheManager;

  @Mock
  Cache liveRatesCache;

  private CurrencyPair currencyPair;
  private List<HistoricalExchangeRate> historicalExchangeRates;
  private List<Map<String, Object>> serverHistoricalExchangeRates;

  @BeforeEach
  public void populateTestData(){
    currencyPair = MockDataCreator.getCurrencyPairForTest();
    historicalExchangeRates = historicalExchangeRatesForTest();
    serverHistoricalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeData.json", Map.class));
    exchangeRateService.setExchangeAPI(exchangeAPI);
    exchangeRateService.setHistoricalExchangeRateServerBaseURL(historicalExchangeRateServerBaseURL);
    lenient().when(cacheManager.getCache("liveRates")).thenReturn(liveRatesCache);
  }

  @Test
  public void testGetHistoricalExchangeRatesFailure(){
    when(currencyPairService.findOrCreatePair("BTC", "USD"))
        .thenReturn(currencyPair);
    when(historicalExchangeRateRepository.findByCurrencyPairAndDateBetweenOrderByDateAsc(any(CurrencyPair.class), any(LocalDate.class), any(LocalDate.class)))
        .thenThrow(new QueryException("Custom JPA Exception", new Exception("Exception in findByCurrencyPairAndDateBetweenOrderByDateAsc")));

    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);

    Assertions.assertThrows(ExchangeRateFetchException.class, () -> {
      exchangeRateService.getHistoricalExchangeRates("BTC", "USD", startDate, endDate);
    });

  }

  @Test
  public void testGetHistoricalExchangeRatesFromLocalStorageSuccess(){
    List<CurrencyExchangeRate> expectedExchangeRates = MockDataCreator.currencyExchangeHistoricalRates(currencyPair);
    when(currencyPairService.findOrCreatePair("BTC", "USD"))
        .thenReturn(currencyPair);
    when(historicalExchangeRateRepository.findByCurrencyPairAndDateBetweenOrderByDateAsc(any(CurrencyPair.class), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(historicalExchangeRates);
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,12);
    List<CurrencyExchangeRate> actualExchangeRates = exchangeRateService.getHistoricalExchangeRates("BTC", "USD", startDate, endDate);

    Assertions.assertEquals(expectedExchangeRates, actualExchangeRates);
    verify(historicalRateCacheService, times(0))
        .saveMissingHistoricalExchangeRates(any(LocalDate.class), any(List.class), any(CurrencyPair.class), any(List.class));
  }

  @Test
  public void testGetHistoricalExchangeRatesFromExchangeServerSuccess(){
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,13);
    String serverExchangeRateFetchURL = "http://xyz.com/historicalRates?fsym=BTC&tsym=USD&limit=1&toTs=1615573800";
    List<CurrencyExchangeRate> expectedExchangeRates = MockDataCreator.currencyExchangeHistoricalRates(currencyPair);
    when(currencyPairService.findOrCreatePair("BTC", "USD"))
        .thenReturn(currencyPair);
    when(historicalExchangeRateRepository.findByCurrencyPairAndDateBetweenOrderByDateAsc(any(CurrencyPair.class), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(historicalExchangeRates);
    doNothing()
        .when(historicalRateCacheService).saveMissingHistoricalExchangeRates(startDate, historicalExchangeRates, currencyPair, serverHistoricalExchangeRates);
    when(restService.get(serverExchangeRateFetchURL, Map.class))
        .thenReturn(serverHistoricalExchangeRates.get(0));

    List<CurrencyExchangeRate> actualExchangeRates = exchangeRateService.getHistoricalExchangeRates("BTC", "USD", startDate, endDate);

    Assertions.assertEquals(4, actualExchangeRates.size());
    verify(historicalRateCacheService, times(1))
        .saveMissingHistoricalExchangeRates(startDate, historicalExchangeRates, currencyPair, serverHistoricalExchangeRates);
  }

  @Test
  public void testGetLiveRateSuccess(){
    Map<String, Object> bitcoinRealTimeExchangeRates = (Map<String, Object>) getData("bitcoinRealTimeExchangeData.json", Map.class);
    when(currencyPairService.findOrCreatePair("BTC", "USD"))
        .thenReturn(currencyPair);
    when(restService.get("http://xyz.com/liveRates?fsym=BTC&tsyms=USD", Map.class))
        .thenReturn(bitcoinRealTimeExchangeRates);

    CurrencyExchangeRate actualResult = exchangeRateService.getLiveRate("BTC", "USD");

    Assertions.assertNotNull(actualResult.getExchangeRate());
    Assertions.assertEquals("BTC", actualResult.getFromCurrency());
    Assertions.assertEquals("USD", actualResult.getToCurrency());
  }

  @Test
  public void testGetLiveRateFailure(){
    when(currencyPairService.findOrCreatePair("BTC", "USD"))
        .thenReturn(currencyPair);
    when(restService.get("http://xyz.com/liveRates?fsym=BTC&tsyms=USD", Map.class))
        .thenThrow(new ExchangeRateFetchException("Rest call exception"));

    Assertions.assertThrows(ExchangeRateFetchException.class, () -> {
      exchangeRateService.getLiveRate("BTC", "USD");
    });
  }

  @Test
  public void testRefreshSubscribedLiveRatesNoActivePairs(){
    when(rateSubscriptionRegistry.getActivePairs()).thenReturn(Set.of());

    exchangeRateService.refreshSubscribedLiveRates();

    verify(restService, times(0)).get(anyString(), any());
    verify(messagingTemplate, times(0)).convertAndSend(anyString(), any(Object.class));
  }

  @Test
  public void testRefreshSubscribedLiveRatesPushesToSubscribers(){
    Map<String, Object> bitcoinRealTimeExchangeRates = (Map<String, Object>) getData("bitcoinRealTimeExchangeData.json", Map.class);
    when(rateSubscriptionRegistry.getActivePairs()).thenReturn(Set.of("BTC_USD"));
    when(restService.get("http://xyz.com/liveRates?fsym=BTC&tsyms=USD", Map.class))
        .thenReturn(bitcoinRealTimeExchangeRates);

    exchangeRateService.refreshSubscribedLiveRates();

    verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/rates/BTC/USD"), any(CurrencyExchangeRate.class));
    verify(liveRatesCache, times(1)).put(eq("BTC_USD"), any(CurrencyExchangeRate.class));
  }

  @Test
  public void testRefreshSubscribedLiveRatesHandlesFetchFailureGracefully(){
    when(rateSubscriptionRegistry.getActivePairs()).thenReturn(Set.of("BTC_USD"));
    when(restService.get("http://xyz.com/liveRates?fsym=BTC&tsyms=USD", Map.class))
        .thenThrow(new ExchangeRateFetchException("Rest call exception"));

    Assertions.assertDoesNotThrow(() -> exchangeRateService.refreshSubscribedLiveRates());

    verify(messagingTemplate, times(0)).convertAndSend(anyString(), any(Object.class));
  }

  @Test
  public void testOnNewRateSubscriptionPushesImmediateRate(){
    Map<String, Object> bitcoinRealTimeExchangeRates = (Map<String, Object>) getData("bitcoinRealTimeExchangeData.json", Map.class);
    when(restService.get("http://xyz.com/liveRates?fsym=BTC&tsyms=USD", Map.class))
        .thenReturn(bitcoinRealTimeExchangeRates);

    exchangeRateService.onNewRateSubscription(new NewRateSubscriptionEvent(this, "BTC", "USD"));

    verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/rates/BTC/USD"), any(CurrencyExchangeRate.class));
  }

}
