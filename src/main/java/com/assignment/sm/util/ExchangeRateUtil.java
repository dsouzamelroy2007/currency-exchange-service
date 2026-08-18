package com.assignment.sm.util;

import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.CurrencyNotFoundException;
import com.assignment.sm.exception.ExchangeRateParseException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.HistoricalRateURLInfo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

@Slf4j
public class ExchangeRateUtil {

  private static final ZoneId zoneId = ZoneId.systemDefault();

  public static CurrencyExchangeRate getCurrencyExchangeRate(String fromCurrency, String toCurrency, Map<String, Object> exchangeRates, CurrencyExchangeRate currencyExchangeRate){
    Object rateValue = exchangeRates.get(toCurrency);
    if(rateValue == null){
      log.error("Error while fetching the exchange rate for currency : "+toCurrency);
      throw new CurrencyNotFoundException(HttpStatus.INTERNAL_SERVER_ERROR, "Target Currency not recognised");
    }
    currencyExchangeRate.setFromCurrency(fromCurrency);
    currencyExchangeRate.setToCurrency(toCurrency);
    currencyExchangeRate.setExchangeRate(Double.valueOf(rateValue.toString()));
    currencyExchangeRate.setDate(LocalDate.now());
    log.info("Exchange rate fetched for {}->{} at {} is : " +currencyExchangeRate.getExchangeRate(), fromCurrency, toCurrency, Instant.now());
    return currencyExchangeRate;
  }

  public static int findMissingDays(int historicalExchangeRateCount, LocalDate startDate, LocalDate endDate){
    return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate)) - historicalExchangeRateCount + 1;
  }

  public static HistoricalRateURLInfo getInfoOnHistoricalRatesToBeFetched(List<HistoricalExchangeRate> historicalExchangeRates, LocalDate startDate, LocalDate endDate){
    long lowerLimitTimeStamp = 0l;
    long upperLimitTimeStamp = 0l;
    int lowerLimit = 0;
    int upperLimit = 0;
    int countOfHistoricalDates = historicalExchangeRates.size();
    if(CollectionUtils.isEmpty(historicalExchangeRates) || historicalExchangeRates.get(0).getDate().equals(startDate)){
      lowerLimitTimeStamp = endDate.atStartOfDay(zoneId).toEpochSecond();
      LocalDate newstartDate = countOfHistoricalDates != 0 ? historicalExchangeRates.get(countOfHistoricalDates - 1).getDate().plusDays(1) : startDate;
      lowerLimit = Math.toIntExact(ChronoUnit.DAYS.between(newstartDate, endDate)) + 1 ;
      log.info(" The lowerLimit in the URL will be the "+ endDate + " and the number of rates to be fetched will be "+lowerLimit);

    }else if(historicalExchangeRates.get(historicalExchangeRates.size() - 1).getDate().equals(endDate)){
      LocalDate previousDay = historicalExchangeRates.get(0).getDate().minusDays(1);
      upperLimitTimeStamp = previousDay.atStartOfDay(zoneId).toEpochSecond();
      upperLimit = Math.toIntExact(ChronoUnit.DAYS.between(startDate, previousDay)) + 1;
      log.info(" The lowerLimit in the URL will be the "+ previousDay + " and the number of rates to be fetched will be "+upperLimit);

    }else{
      log.info(" Two limits are identified as the start and end Dates of the available historical rates in the DB are not equal to the requested dates");
      LocalDate previousDay = historicalExchangeRates.get(0).getDate().minusDays(1);
      upperLimitTimeStamp = previousDay.atStartOfDay(zoneId).toEpochSecond();
      upperLimit = Math.toIntExact(ChronoUnit.DAYS.between(startDate, previousDay)) + 1;
      lowerLimitTimeStamp = endDate.atStartOfDay(zoneId).toEpochSecond();
      lowerLimit = Math.toIntExact(ChronoUnit.DAYS.between(historicalExchangeRates.get(historicalExchangeRates.size() - 1).getDate(), endDate));
    }

    return HistoricalRateURLInfo.builder()
                                  .lowerLimit(lowerLimit)
                                  .lowerLimitTimeStamp(lowerLimitTimeStamp != 0 ? lowerLimitTimeStamp : null)
                                  .upperLimit(upperLimit)
                                  .upperLimitTimeStamp(upperLimitTimeStamp != 0 ? upperLimitTimeStamp : null)
                                  .build();
  }

  public static String getURLStringToFetchHistoricalExchangeRates(String exchangeRateBaseUrl,
                                                                  String fromCurrencyName,
                                                                  String toCurrencyName,
                                                                  int limit,
                                                                  Long toTimeStamp)
  {
    StringBuilder historicalExchangeRateURL = new StringBuilder();
    historicalExchangeRateURL.append(exchangeRateBaseUrl);
    historicalExchangeRateURL.append("?fsym="+fromCurrencyName);
    historicalExchangeRateURL.append("&tsym="+toCurrencyName);
    historicalExchangeRateURL.append("&limit="+limit);
    historicalExchangeRateURL.append("&toTs="+toTimeStamp);
    log.info(" The URL formed to Fetch Historical Exchange Rates from Server: "+historicalExchangeRateURL.toString());
    return historicalExchangeRateURL.toString();
  }

  public static String getURLStringToFetchRealTimeExchangeRate(String exchangeRateBaseUrl, String fromCurrencyName, List<String> toCurrencyNames){
    StringBuilder realTimeExchangeRateURL = new StringBuilder();
    realTimeExchangeRateURL.append(exchangeRateBaseUrl);
    realTimeExchangeRateURL.append("?fsym="+fromCurrencyName);
    realTimeExchangeRateURL.append("&tsyms="+String.join(",", toCurrencyNames));
    log.info(" The URL formed to Fetch Real Time Exchange Rates from Server: "+realTimeExchangeRateURL.toString());
    return realTimeExchangeRateURL.toString();
  }

  public static List<HistoricalExchangeRate> getHistoricalExchangeRatesToBeSaved(LocalDate startDate, List<HistoricalExchangeRate> existingHistoricalRates, CurrencyPair currencyPair, List<Map<String,Object>> bitcoinHistoricalRateList)
  {
      Set<LocalDate> uniqueDates = existingHistoricalRates.stream()
                                                          .map(rate -> rate.getDate())
                                                          .collect(Collectors.toSet());
      String fromAbbreviation = currencyPair.getFromCurrency().getAbbreviation();
      String toAbbreviation = currencyPair.getToCurrency().getAbbreviation();
      final List<CurrencyExchangeRate> currencyExchangeRates = parseCurrencyExchangeRates(fromAbbreviation, toAbbreviation, startDate, bitcoinHistoricalRateList, uniqueDates);
      final LocalDateTime timestamp = LocalDateTime.now();
      return currencyExchangeRates.stream()
          .map(currencyExchangeRate -> getHistoricalExchangeRateFromCurrencyExchangeRateObject(currencyPair, currencyExchangeRate, timestamp))
          .collect(Collectors.toList());

  }

  public static List<CurrencyExchangeRate> getHistoricalExchangeRatesToCurrencyExchangeRateDTO(LocalDate startDate,
                                                                                              CurrencyPair currencyPair,
                                                                                              List<HistoricalExchangeRate> existingHistoricalRates,
                                                                                              List<Map<String,Object>> bitcoinHistoricalRateList){

      String fromAbbreviation = currencyPair.getFromCurrency().getAbbreviation();
      String toAbbreviation = currencyPair.getToCurrency().getAbbreviation();

      final List<CurrencyExchangeRate> historicalExchangeRateList = existingHistoricalRates.stream()
                                                              .map(rate -> new CurrencyExchangeRate(fromAbbreviation,
                                                                                                    toAbbreviation,
                                                                                                    rate.getRate(),
                                                                                                    rate.getDate()
                                                                  )
                                                              )
                                                              .collect(Collectors.toList());
       Set<LocalDate> uniqueDates = existingHistoricalRates.stream()
                                                              .map(rate -> rate.getDate())
                                                              .collect(Collectors.toSet());
      historicalExchangeRateList.addAll(parseCurrencyExchangeRates(fromAbbreviation, toAbbreviation, startDate, bitcoinHistoricalRateList, uniqueDates));
      historicalExchangeRateList.sort(Comparator.comparing( CurrencyExchangeRate :: getDate, LocalDate::compareTo));
      return historicalExchangeRateList;
  }

  private static List<CurrencyExchangeRate> parseCurrencyExchangeRates(String fromCurrencyAbbreviation, String toCurrencyAbbreviation, LocalDate startDate, List<Map<String,Object>> bitcoinHistoricalRateList, Set<LocalDate> uniqueDates){

    final List<CurrencyExchangeRate> currencyExchangeRates = new ArrayList<>();
    if(!CollectionUtils.isEmpty(bitcoinHistoricalRateList)){

      bitcoinHistoricalRateList.forEach(bitcoinHistoricalExhangeRates -> {
            try {
              List<Map<String, Object>> historicalRatesFromServer = (List<Map<String, Object>>) ((Map<String, Object>) bitcoinHistoricalExhangeRates
                  .get("Data")).get("Data");
              log.info(
                  "Rates fetched from the exchange server: " + historicalRatesFromServer.size());
              historicalRatesFromServer.stream()
                  .forEach(rate -> {
                        Double exchangeRate = Double.valueOf(rate.get("close").toString());
                        long epoch = Long.valueOf(rate.get("time").toString());
                        LocalDate rateDate = Instant.ofEpochMilli(epoch * 1000)
                            .atZone(zoneId)
                            .toLocalDate();
                        if (!rateDate.isBefore(startDate) && !uniqueDates.contains(rateDate)) {
                          currencyExchangeRates.add(new CurrencyExchangeRate(
                              fromCurrencyAbbreviation,
                              toCurrencyAbbreviation,
                              exchangeRate,
                              rateDate));
                        }
                      }
                  );

            } catch (Exception e) {
              log.error("Error while parsing historical server rates", e);
              throw new ExchangeRateParseException(HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
          }
      );
    }
    return currencyExchangeRates;
  }

  private static HistoricalExchangeRate getHistoricalExchangeRateFromCurrencyExchangeRateObject(CurrencyPair currencyPair, CurrencyExchangeRate currencyExchangeRate, LocalDateTime timestamp){
    return new HistoricalExchangeRate(null,
                                        currencyExchangeRate.getExchangeRate(),
                                        currencyExchangeRate.getDate(),
                                        timestamp,
                                        currencyPair);
  }
}
