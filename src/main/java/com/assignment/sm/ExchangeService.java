package com.assignment.sm;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.time.Duration;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableAsync
public class ExchangeService {

  public static void main(String[] args) {
    // Must happen before the Spring context starts: the DataSource/connection pool (and H2's
    // JDBC driver, whose Date conversions fall back to Calendar.getInstance()) get created
    // during context startup, so setting this in @PostConstruct was too late and left DB
    // connections pinned to the JVM's original default zone, causing DATE columns to read back
    // shifted by a day around DST boundaries.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(ExchangeService.class, args);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {

    return builder
            .setConnectTimeout(Duration.ofMillis(10000))
            .setReadTimeout(Duration.ofMillis(10000))
            .build();
  }

  @Bean("threadPoolTaskExecutor")
  public TaskExecutor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(100);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("Async-");
    return executor;
  }

  @Bean
  public OpenAPI apiInfo() {
    return new OpenAPI().info(new Info()
        .title("Currency Exchange Service")
        .description("Real time & Historical exchange rates")
        .version("1.0")
        .contact(new Contact().name("dev_team").email("tech-support@.com")));
  }

}
