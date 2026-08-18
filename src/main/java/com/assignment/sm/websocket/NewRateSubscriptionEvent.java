package com.assignment.sm.websocket;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewRateSubscriptionEvent extends ApplicationEvent {

  private final String fromCurrency;
  private final String toCurrency;

  public NewRateSubscriptionEvent(Object source, String fromCurrency, String toCurrency) {
    super(source);
    this.fromCurrency = fromCurrency;
    this.toCurrency = toCurrency;
  }
}
