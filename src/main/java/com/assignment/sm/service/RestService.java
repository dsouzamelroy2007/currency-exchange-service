package com.assignment.sm.service;

import com.assignment.sm.exception.ExchangeRateFetchException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RestService {

    @Autowired
    RestTemplate restTemplate;

    final HttpHeaders jsonHttpHeader = new HttpHeaders();
    {
        jsonHttpHeader.setContentType(MediaType.APPLICATION_JSON);
        List<MediaType> mediaTypeList = new ArrayList<>();
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        jsonHttpHeader.setAccept(mediaTypeList);
    }


    public <T> T get(String url, Class<T> returnType) {
        try {
            ResponseEntity<T> responseEntity = restTemplate.getForEntity(url, returnType);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity.getBody();
            } else {
                throw new ExchangeRateFetchException(HttpStatus.INTERNAL_SERVER_ERROR,"Exception in get request error code: " + responseEntity.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.error("Exception while fetching exchange rates ", e);
            throw new ExchangeRateFetchException(HttpStatus.INTERNAL_SERVER_ERROR, e.getCause());
        }
    }


}
