package com.assignment.sm.model;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyExchangeRate implements Serializable {

  private String toCurrency;
  private Double exchangeRate;

  private LocalDate date;

  public CurrencyExchangeRate(String currency){
    this.toCurrency = currency;
  }
}
