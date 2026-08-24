package com.assignment.sm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  public Currency(String name, String abbreviation){
    this.name = name;
    this.abbreviation = abbreviation;
  }
}
