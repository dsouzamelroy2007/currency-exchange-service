package com.assignment.sm.util;


import static com.assignment.sm.util.ExchangeRateUtil.findMissingDays;
import static com.assignment.sm.util.ExchangeRateUtil.getCurrencyExchangeRate;
import static com.assignment.sm.util.ExchangeRateUtil.getHistoricalExchangeRatesToBeSaved;
import static com.assignment.sm.util.ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO;
import static com.assignment.sm.util.ExchangeRateUtil.getInfoOnHistoricalRatesToBeFetched;
import static com.assignment.sm.util.ExchangeRateUtil.getURLStringToFetchHistoricalExchangeRates;
import static com.assignment.sm.util.MockDataCreator.currencyExchangeLiveRate;
import static com.assignment.sm.util.MockDataCreator.getCurrencyObjectForTest;
import static com.assignment.sm.util.MockDataCreator.getData;
import static com.assignment.sm.util.MockDataCreator.historicalExchangeRatesForTest;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.CurrencyNotFoundException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.HistoricalRateURLInfo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExchangeRateUtilTest {

  private Map<String, Object> bitcoinRealTimeExchangeRates;
  private CurrencyExchangeRate currencyExchangeRate;
  private List<HistoricalExchangeRate> historicalExchangeRates;

  @BeforeEach
  public void populateTestData(){
    historicalExchangeRates = historicalExchangeRatesForTest();
    bitcoinRealTimeExchangeRates = (Map<String, Object>) getData("bitcoinRealTimeExchangeData.json", Map.class);
    currencyExchangeRate = currencyExchangeLiveRate();
  }

  @Test
  public void testGetCurrencyExchangeRateFailure(){
    currencyExchangeRate.setExchangeRate(56648.74);
    Assertions.assertThrows(CurrencyNotFoundException.class, () -> {
      getCurrencyExchangeRate("USD$", bitcoinRealTimeExchangeRates, new CurrencyExchangeRate());
    });
  }

  @Test
  public void testGetCurrencyExchangeRateSuccess(){
    CurrencyExchangeRate actualResult = getCurrencyExchangeRate("USD", bitcoinRealTimeExchangeRates, new CurrencyExchangeRate());
    Assertions.assertEquals(currencyExchangeRate.getExchangeRate(), actualResult.getExchangeRate());
  }


  @Test
  public void testFindMissingDaysFailure(){
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);
    List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRatesForTest();
    int actualResult = findMissingDays(historicalExchangeRates.size(), startDate, endDate);
    Assertions.assertNotEquals(0, actualResult);
  }

  @Test
  public void testFindMissingDaysSuccess(){
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,12);
    List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRatesForTest();
    int actualResult = findMissingDays(historicalExchangeRates.size(), startDate, endDate);
    Assertions.assertEquals(0, actualResult);
  }

  @Test
  public void testGetInfoOnHistoricalRatesToBeFetchedFailure(){
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);
    List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRatesForTest();
    HistoricalRateURLInfo actualResult = getInfoOnHistoricalRatesToBeFetched(historicalExchangeRates, startDate, endDate);
    Assertions.assertNotNull(actualResult.getLowerLimitTimeStamp());
    Assertions.assertEquals(2, actualResult.getLowerLimit());
    Assertions.assertNull(actualResult.getUpperLimitTimeStamp());
  }

  @Test
  public void testGetInfoOnHistoricalRatesToBeFetchedSuccess(){
    LocalDate startDate = LocalDate.of(2021,03,9);
    LocalDate endDate = LocalDate.of(2021,03,14);
    HistoricalRateURLInfo actualResult = getInfoOnHistoricalRatesToBeFetched(historicalExchangeRates, startDate, endDate);
    Assertions.assertNotNull(actualResult.getLowerLimitTimeStamp());
    Assertions.assertEquals(2, actualResult.getLowerLimit());
    Assertions.assertNotNull(actualResult.getUpperLimitTimeStamp());
    Assertions.assertEquals(1, actualResult.getUpperLimit());
  }

  @Test
  public void testURLStringToFetchHistoricalExchangeRatesFailure(){
    String baseURl = "http://xyz.com/historical";
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);
    String actualResult = getURLStringToFetchHistoricalExchangeRates(baseURl, "BTC", "USD", 10, 1615660200l);
    Assertions.assertNotEquals("http://xyz.com/historical", actualResult);
  }

  @Test
  public void testURLStringToFetchHistoricalExchangeRatesSuccess(){
    String baseURl = "http://xyz.com/historical";
    LocalDate startDate = LocalDate.of(2021,03,10);
    LocalDate endDate = LocalDate.of(2021,03,14);
    List<HistoricalExchangeRate> historicalExchangeRates = historicalExchangeRatesForTest();
    String actualResult = getURLStringToFetchHistoricalExchangeRates(baseURl, "BTC", "USD", 10, 1615636416l);
    Assertions.assertEquals("http://xyz.com/historical?fsym=BTC&tsym=USD&limit=10&toTs=1615636416", actualResult);
  }

  @Test
  public void testGetHistoricalExchangeRatesToBeSavedFailure(){
    Currency currency = getCurrencyObjectForTest();
    LocalDate startDate = LocalDate.of(2021, 3, 10);

    List<Map<String, Object>> historicalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeBadData.json", Map.class));
    Assertions.assertThrows(NullPointerException.class, () -> {
      getHistoricalExchangeRatesToBeSaved(startDate, new ArrayList<>(), currency, historicalExchangeRates);
    });
  }

  @Test
  public void testGetHistoricalExchangeRatesToBeSavedSuccess(){
    Currency currency = getCurrencyObjectForTest();
    LocalDate startDate = LocalDate.of(2021, 3, 10);

    Map<String, Object> historicalExchangeRates = (Map<String, Object>) getData("historicalExchangeData.json", Map.class);
    List<HistoricalExchangeRate> actualHistoricalExchangeRates = getHistoricalExchangeRatesToBeSaved(startDate, new ArrayList<>(), currency, List.of(historicalExchangeRates));
    Assertions.assertEquals(4, actualHistoricalExchangeRates.size());
  }

  @Test
  public void testGetHistoricalExchangeRatesToCurrencyExchangeRateDTOFailure(){
    LocalDate startDate = LocalDate.of(2021, 3, 10);
    List<Map<String, Object>> bitcoinHistoricalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeBadData.json", Map.class));
    Assertions.assertThrows(NullPointerException.class, () -> {
      getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate,"USD", historicalExchangeRates, bitcoinHistoricalExchangeRates);
    });
  }

  @Test
  public void testGetHistoricalExchangeRatesToCurrencyExchangeRateDTOSuccess(){
    LocalDate startDate = LocalDate.of(2021, 3, 10);
    List<Map<String, Object>> bitcoinHistoricalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeData.json", Map.class));
    List<CurrencyExchangeRate> actualHistoricalExchangeRates = getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate,"USD", historicalExchangeRates, bitcoinHistoricalExchangeRates);
    Assertions.assertEquals(4, actualHistoricalExchangeRates.size());
  }
}
