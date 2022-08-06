package com.assignment.sm.repository;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.HistoricalExchangeRate;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricalExchangeRateRepository extends JpaRepository<HistoricalExchangeRate, Long> {

  List<HistoricalExchangeRate> findByCurrencyAndDateBetweenOrderByDateAsc(Currency currency, LocalDate startDate, LocalDate endDate);
}