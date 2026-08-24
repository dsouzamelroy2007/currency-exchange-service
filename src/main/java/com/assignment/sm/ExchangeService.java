package com.assignment.sm;

import java.time.Duration;
import java.util.Collections;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableAsync
@EnableSwagger2
@EnableHystrix
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
  public Docket apis(){
    return new Docket(DocumentationType.SWAGGER_2)
                                  .select()
                                  .apis(RequestHandlerSelectors.basePackage("com.assignment.sm.controller"))
                                  .paths(PathSelectors.any())
                                  .build()
                                  .pathMapping("")
                                  .apiInfo(getApiInfo());
  }

  private ApiInfo getApiInfo() {
    return new ApiInfo("Currency Exchange Service",
                "Real time & Historical exchange rates",
                   "1.0","wwww.xyz.com",
                            new Contact("dev_team","","tech-support@.com"),
                    null,
                  null,
                  Collections.emptyList());
  }

}
