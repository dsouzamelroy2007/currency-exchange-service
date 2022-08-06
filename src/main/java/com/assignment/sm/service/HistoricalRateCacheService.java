package com.assignment.sm.service;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import com.assignment.sm.util.ExchangeRateUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
  public void saveMissingHistoricalExchangeRates(LocalDate startDate, List<HistoricalExchangeRate> existingHistoricalDates, Currency currency, List<Map<String, Object>> bitcoinHistoricalExhangeRates){
    try{
      List<HistoricalExchangeRate> missingHistoricalRates = ExchangeRateUtil.getHistoricalExchangeRatesToBeSaved(startDate, existingHistoricalDates, currency, bitcoinHistoricalExhangeRates);
      historicalExchangeRateRepository.saveAll(missingHistoricalRates);
    }catch (Exception e){
      log.error("Error while saving missing historical dates to local storage",e);
      throw new ExchangeRateSaveException(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause());
    }
    log.info("Missing historical dates saved successfully");
  }
}
