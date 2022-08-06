package com.assignment.sm.service;

import static com.assignment.sm.util.MockDataCreator.getData;
import static org.mockito.Mockito.when;

import com.assignment.sm.exception.ExchangeRateFetchException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class RestServiceTest {

  @InjectMocks
  RestService restService;

  @Mock
  RestTemplate restTemplate;

  @Test
  public void testGetResourceFailure(){
    Throwable throwable = new Throwable("GatewayTime Error");
    Exception exception = new Exception("Rest call exception", throwable);
    when(restTemplate.getForEntity("getURL", Map.class))
        .thenThrow(new RuntimeException("Error while", exception.getCause()));

    Assertions.assertThrows(ExchangeRateFetchException.class, () -> {
      restService.get("getURL", Map.class);
    });
  }

  @Test
  public void testGetResourceSuccess(){
    ResponseEntity<Map> responseEntity = new ResponseEntity<>( getData("bitcoinRealTimeExchangeData.json", Map.class), HttpStatus.OK);

    when(restTemplate.getForEntity("getURL", Map.class))
        .thenReturn(responseEntity);

    Map actualResponseEntity = restService.get("getURL", Map.class);
    Assertions.assertEquals(responseEntity.getBody(), actualResponseEntity);

  }
}
