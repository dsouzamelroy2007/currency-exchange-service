package com.assignment.sm.domain;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
