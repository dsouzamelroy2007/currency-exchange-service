package com.assignment.sm.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class ExchangeRateResponse implements Serializable {
  private String fromCurrency;

  List<CurrencyExchangeRate> exchangeRates;

}
