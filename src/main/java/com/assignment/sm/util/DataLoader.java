package com.assignment.sm.util;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements ApplicationRunner {

  @Value("${toCurrency}")
  private String targetCurrency;

  private CurrencyRepository currencyRepository;

  @Autowired
  public DataLoader(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  private Currency createCurrency(){
    return new Currency("United States dollar", targetCurrency);
  }

  public void run(ApplicationArguments args) {
    currencyRepository.save(createCurrency());
  }

}