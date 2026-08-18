package com.assignment.sm.repository;

import com.assignment.sm.domain.CurrencyPair;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyPairRepository extends JpaRepository<CurrencyPair, Long> {

  Optional<CurrencyPair> findByFromCurrency_AbbreviationAndToCurrency_Abbreviation(String fromAbbreviation, String toAbbreviation);

}
