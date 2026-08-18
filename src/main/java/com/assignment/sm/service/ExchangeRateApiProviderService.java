package com.assignment.sm.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Free, keyless live-rate provider (https://www.exchangerate-api.com/docs/free). Fiat-only and
 * updates once every 24h, so results are cached per base currency for that long; callers fall
 * back to the configured exchangeAPI when this returns null (unsupported base or unreachable).
 */
@Service
@Slf4j
public class ExchangeRateApiProviderService {

  @Value("${externalRatesApi.baseUrl}")
  private String baseUrl;

  @Autowired
  RestService restService;

  @Cacheable(value = "externalBaseRates", key = "#from", unless = "#result == null")
  public Map<String, Object> getRatesForBase(String from) {
    try {
      Map<String, Object> response = restService.get(baseUrl + "/" + from, Map.class);
      if (response != null && "success".equals(response.get("result"))) {
        return (Map<String, Object>) response.get("rates");
      }
      log.info("Free exchange rate provider has no data for base {}: {}", from, response);
    } catch (Exception e) {
      log.warn("Free exchange rate provider request failed for base {}, falling back to configured exchange API", from, e);
    }
    return null;
  }
}
