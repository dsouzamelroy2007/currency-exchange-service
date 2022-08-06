package com.assignment.sm.service;

import static com.assignment.sm.util.MockDataCreator.getData;
import static com.assignment.sm.util.MockDataCreator.historicalExchangeRatesForTest;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.repository.CurrencyRepository;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.MockDataCreator;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.hibernate.QueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

  private final String targetCurrency = "USD";

  private final String exchangeAPI = "http://xyz.com/liveRates";

  private final String historicalExchangeRateServerBaseURL = "http://xyz.com/historicalRates";

  private final String fromCurrency = "BTC";

  @InjectMocks
  ExchangeRateService exchangeRateService;

  @Mock
  RestService restService;

  @Mock
  CurrencyRepository currencyRepository;

  @Mock
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  @Mock
  HistoricalRateCacheService historicalRateCacheService;

  @Spy
  CurrencyExchangeRate currencyExchangeRate;

  private Currency currency;
  private List<HistoricalExchangeRate> historicalExchangeRates;
  private List<Map<String, Object>> serverHistoricalExchangeRates;

  @BeforeEach
  public void populateTestData(){
    currency = MockDataCreator.getCurrencyObjectForTest();
    historicalExchangeRates = historicalExchangeRatesForTest();
    serverHistoricalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeData.json", Map.class));
    exchangeRateService.setFromCurrency(fromCurrency);
    exchangeRateService.setExchangeAPI(exchangeAPI);
    exchangeRateService.setTargetCurrency(targetCurrency);
    exchangeRateService.setHistoricalExchangeRateServerBaseURL(historicalExchangeRateServerBaseURL);
    currencyExchangeRate = exchangeRateService.getCurrencyExchangeRate();
  }

  @Test
  public void testGetHistoricalExchangeRatesFailure(){
    when(currencyRepository.findByAbbreviation(anyString()))
        .thenReturn(currency);
    when(historicalExchangeRateRepository.findByCurrencyAndDateBetweenOrderByDateAsc(any(Currency.class), any(LocalDate.class), any(LocalDate.class)))
        .thenThrow(new QueryException("Custom JPA Exception", new Exception("Exception in findByCurrencyAndDateBetweenOrderByDateAsc")));

    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);

    Assertions.assertThrows(ExchangeRateFetchException.class, () -> {
      exchangeRateService.getHistoricalExchangeRates("USD", startDate, endDate);
    });

  }

  @Test
  public void testGetHistoricalExchangeRatesFromLocalStorageSuccess(){
    List<CurrencyExchangeRate> expectedExchangeRates = MockDataCreator.currencyExchangeHistoricalRates(currency);
    when(currencyRepository.findByAbbreviation(anyString()))
        .thenReturn(currency);
    when(historicalExchangeRateRepository.findByCurrencyAndDateBetweenOrderByDateAsc(any(Currency.class), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(historicalExchangeRates);
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,12);
    List<CurrencyExchangeRate> actualExchangeRates = exchangeRateService.getHistoricalExchangeRates("USD", startDate, endDate);

    Assertions.assertEquals(expectedExchangeRates, actualExchangeRates);
    verify(historicalRateCacheService, times(0))
        .saveMissingHistoricalExchangeRates(any(LocalDate.class), any(List.class), any(Currency.class), any(List.class));
  }

  @Test
  public void testGetHistoricalExchangeRatesFromExchangeServerSuccess(){
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,13);
    String serverExchangeRateFetchURL = "http://xyz.com/historicalRates?fsym=BTC&tsym=USD&limit=1&toTs=1615573800";
    List<CurrencyExchangeRate> expectedExchangeRates = MockDataCreator.currencyExchangeHistoricalRates(currency);
    when(currencyRepository.findByAbbreviation(anyString()))
        .thenReturn(currency);
    when(historicalExchangeRateRepository.findByCurrencyAndDateBetweenOrderByDateAsc(any(Currency.class), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(historicalExchangeRates);
    doNothing()
        .when(historicalRateCacheService).saveMissingHistoricalExchangeRates(startDate, historicalExchangeRates, currency, serverHistoricalExchangeRates);
    when(restService.get(serverExchangeRateFetchURL, Map.class))
        .thenReturn(serverHistoricalExchangeRates.get(0));

    List<CurrencyExchangeRate> actualExchangeRates = exchangeRateService.getHistoricalExchangeRates("USD", startDate, endDate);

    Assertions.assertEquals(4, actualExchangeRates.size());
    verify(historicalRateCacheService, times(1))
        .saveMissingHistoricalExchangeRates(startDate, historicalExchangeRates, currency, serverHistoricalExchangeRates);
  }

  @Test
  public void testFetchBitCoinExchangeRateFailure(){
    Assertions.assertNull(currencyExchangeRate.getExchangeRate());
    Throwable throwable = new Throwable("GatewayTime Error");
    Exception exception = new Exception("Rest call exception", throwable);
    when(restService.get(exchangeAPI, Map.class))
        .thenThrow(new RuntimeException("Error while", exception.getCause()));

    Assertions.assertThrows(ExchangeRateFetchException.class, () -> {
      exchangeRateService.fetchBitCoinExchangeRate();
    });
  }

  @Test
  public void testFetchBitCoinExchangeRateSuccess(){
    Assertions.assertNull(currencyExchangeRate.getExchangeRate());

    Map<String, Object> bitcoinRealTimeExchangeRates = (Map<String, Object>) getData(
        "bitcoinRealTimeExchangeData.json", Map.class);
    when(restService.get(exchangeAPI, Map.class))
        .thenReturn(bitcoinRealTimeExchangeRates);
    exchangeRateService.fetchBitCoinExchangeRate();
    Assertions.assertNotNull(currencyExchangeRate.getExchangeRate());
  }

}
