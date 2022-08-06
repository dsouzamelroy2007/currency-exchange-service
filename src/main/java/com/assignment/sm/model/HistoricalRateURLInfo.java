package com.assignment.sm.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@Builder
public class HistoricalRateURLInfo {

  private Long lowerLimitTimeStamp;
  private int lowerLimit;
  private Long upperLimitTimeStamp;
  private int upperLimit;
}
