package com.assignment.sm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

  @Value("${cache.liveRates.ttlSeconds}")
  private long liveRatesTtlSeconds;

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("liveRates");
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(liveRatesTtlSeconds)));
    return cacheManager;
  }
}
