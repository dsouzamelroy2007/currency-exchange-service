package com.assignment.sm.repository;

import com.assignment.sm.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

  Currency findByAbbreviation(String currencyName);

}