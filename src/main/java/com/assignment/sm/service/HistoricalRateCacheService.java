package com.assignment.sm.service;

import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.ExchangeRateUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class HistoricalRateCacheService {

  @Autowired
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  @Async("threadPoolTaskExecutor")
  @Transactional
  public void saveMissingHistoricalExchangeRates(LocalDate startDate, List<HistoricalExchangeRate> existingHistoricalDates, CurrencyPair currencyPair, List<Map<String, Object>> bitcoinHistoricalExhangeRates){
    try{
      List<HistoricalExchangeRate> missingHistoricalRates = ExchangeRateUtil.getHistoricalExchangeRatesToBeSaved(startDate, existingHistoricalDates, currencyPair, bitcoinHistoricalExhangeRates);
      historicalExchangeRateRepository.saveAll(missingHistoricalRates);
    }catch (Exception e){
      log.error("Error while saving missing historical dates to local storage",e);
      throw new ExchangeRateSaveException(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
    log.info("Missing historical dates saved successfully");
  }

  @Async("threadPoolTaskExecutor")
  @Transactional
  public void recordTodaysRateIfMissing(CurrencyPair currencyPair, CurrencyExchangeRate rate){
    LocalDate today = LocalDate.now();
    if(historicalExchangeRateRepository.existsByCurrencyPairAndDate(currencyPair, today)){
      return;
    }
    try{
      historicalExchangeRateRepository.save(new HistoricalExchangeRate(null, rate.getExchangeRate(), today, LocalDateTime.now(), currencyPair));
      log.info("Recorded today's live rate as historical data point for pair {}/{}", rate.getFromCurrency(), rate.getToCurrency());
    }catch (DataIntegrityViolationException e){
      log.debug("Today's historical rate for pair {}/{} was already recorded concurrently", rate.getFromCurrency(), rate.getToCurrency());
    }
  }
}
