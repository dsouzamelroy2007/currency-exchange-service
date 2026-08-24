package com.assignment.sm.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = @Index(columnList = "date", name = "exchange_rate_history_date"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"currency_pair_id", "date"}))
public class HistoricalExchangeRate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rate")
  private Double rate;

  @Column(name = "date")
  private LocalDate date;

  @Column(name = "last_updated")
  private LocalDateTime updatedTime;

  @ManyToOne
  @JoinColumn(name = "currency_pair_id")
  @JsonIgnore
  private CurrencyPair currencyPair;

}
