package com.assignment.sm.service;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.repository.CurrencyPairRepository;
import com.assignment.sm.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyPairService {

  @Autowired
  CurrencyRepository currencyRepository;

  @Autowired
  CurrencyPairRepository currencyPairRepository;

  @Transactional
  public CurrencyPair findOrCreatePair(String from, String to){
    String fromAbbreviation = from.toUpperCase();
    String toAbbreviation = to.toUpperCase();
    if(fromAbbreviation.equals(toAbbreviation)){
      throw new InvalidInputException(HttpStatus.BAD_REQUEST, "fromCurrency and toCurrency must not be the same");
    }
    return currencyPairRepository.findByFromCurrency_AbbreviationAndToCurrency_Abbreviation(fromAbbreviation, toAbbreviation)
        .orElseGet(() -> {
          Currency fromCurrency = findOrCreateCurrency(fromAbbreviation);
          Currency toCurrency = findOrCreateCurrency(toAbbreviation);
          return currencyPairRepository.save(new CurrencyPair(fromCurrency, toCurrency));
        });
  }

  private Currency findOrCreateCurrency(String abbreviation){
    Currency currency = currencyRepository.findByAbbreviation(abbreviation);
    if(currency != null){
      return currency;
    }
    return currencyRepository.save(new Currency(abbreviation, abbreviation));
  }
}
