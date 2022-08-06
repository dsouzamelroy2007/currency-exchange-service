package com.assignment.sm.service;

import static com.assignment.sm.util.MockDataCreator.getCurrencyObjectForTest;
import static com.assignment.sm.util.MockDataCreator.getData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.exception.ExchangeRateSaveException;
import com.assignment.sm.repository.HistoricalExchangeRateRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class HistoricalRateCacheServiceTest {

  @InjectMocks
  HistoricalRateCacheService historicalRateCacheService;

  @Mock
  HistoricalExchangeRateRepository historicalExchangeRateRepository;

  private Currency currency;
  private LocalDate startDate;
  private List<Map<String, Object>> historicalExchangeRates;

  @BeforeEach
  public void populateTestData(){
     currency = getCurrencyObjectForTest();
     startDate = LocalDate.of(2021, 3, 10);

     historicalExchangeRates = List.of((Map<String, Object>) getData("historicalExchangeData.json", Map.class));

  }

  @Test
  public void testsaveMissingHistoricalExchangeRatesFailure(){
    SQLException sqlException = new SQLException("DB Constraint violated", "23000");
    ConstraintViolationException cve = new ConstraintViolationException(
        "SQL integrity constraint violation", sqlException, "integrity_constraint_violation");
    when(historicalExchangeRateRepository.saveAll(any(List.class)))
        .thenThrow(new DataIntegrityViolationException("DataIntegrity Error", cve));

    Assertions.assertThrows(ExchangeRateSaveException.class, () -> {
      historicalRateCacheService.saveMissingHistoricalExchangeRates(startDate, new ArrayList<>(), currency, historicalExchangeRates);
    });
  }

  @Test
  public void testsaveMissingHistoricalExchangeRatesSuccess(){

    when(historicalExchangeRateRepository.saveAll(any(List.class)))
        .thenReturn(new ArrayList<>());

    historicalRateCacheService.saveMissingHistoricalExchangeRates(startDate, new ArrayList<>(), currency, historicalExchangeRates);
    verify(historicalExchangeRateRepository, times(1)).saveAll(any(List.class));

  }
}
