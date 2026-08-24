package com.assignment.sm.domain;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currency_pair",
    uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency_id", "to_currency_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CurrencyPair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "from_currency_id")
  private Currency fromCurrency;

  @ManyToOne
  @JoinColumn(name = "to_currency_id")
  private Currency toCurrency;

  @OneToMany(mappedBy = "currencyPair",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL)
  private List<HistoricalExchangeRate> rates;

  public CurrencyPair(Currency fromCurrency, Currency toCurrency){
    this.fromCurrency = fromCurrency;
    this.toCurrency = toCurrency;
  }
}
