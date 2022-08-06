package com.assignment.sm.domain;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "currency")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Currency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name")
  private String name;

  @Column(unique = true, name = "abbreviation")
  private String abbreviation;

  @OneToMany(mappedBy = "currency",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL)
  private List<HistoricalExchangeRate> rates;

  public Currency(String name, String abbreviation){
    this.name = name;
    this.abbreviation = abbreviation;
  }
}
