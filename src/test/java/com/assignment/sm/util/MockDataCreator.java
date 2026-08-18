package com.assignment.sm.util;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.domain.HistoricalExchangeRate;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.ExchangeRateResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class MockDataCreator {

    private static LocalDate startDate = LocalDate.of(2021, 3, 10);

    public static Currency getFromCurrencyObjectForTest(){
        return new Currency("Bitcoin", "BTC");
    }

    public static Currency getCurrencyObjectForTest(){
        return new Currency("United States Dollar", "USD");
    }

    public static CurrencyPair getCurrencyPairForTest(){
        return new CurrencyPair(getFromCurrencyObjectForTest(), getCurrencyObjectForTest());
    }

    public static List<HistoricalExchangeRate> historicalExchangeRatesForTest(){
        CurrencyPair currencyPair = getCurrencyPairForTest();
        return List.of(
            new HistoricalExchangeRate(1L , 56988.45, LocalDate.of(2021, 3, 10), LocalDateTime.now(), currencyPair ),
            new HistoricalExchangeRate(2L , 56868.45, LocalDate.of(2021, 3, 11), LocalDateTime.now(), currencyPair ),
            new HistoricalExchangeRate(3L , 56788.45, LocalDate.of(2021, 3, 12), LocalDateTime.now(), currencyPair )
        );
    }

    public static CurrencyExchangeRate currencyExchangeLiveRate(){
        return new CurrencyExchangeRate("BTC", "USD", 56648.74, LocalDate.now());
    }

    public static ExchangeRateResponse getResponseForLiveRate(){
        return new ExchangeRateResponse(List.of(currencyExchangeLiveRate()));
    }

    public static List<CurrencyExchangeRate> currencyExchangeHistoricalRates(CurrencyPair currencyPair){
        return ExchangeRateUtil.getHistoricalExchangeRatesToCurrencyExchangeRateDTO(startDate, currencyPair, historicalExchangeRatesForTest(), null);
    }

    public static ExchangeRateResponse getResponseForHistoricalRates(){
        CurrencyPair currencyPair = getCurrencyPairForTest();
        List<CurrencyExchangeRate> exchangeRates = currencyExchangeHistoricalRates(currencyPair);
        return new ExchangeRateResponse(exchangeRates);
    }

    public static <T, E> T getData(String filename, Class<T> dataClass) {
        String data = "";
        try {
            Resource resource = new ClassPathResource("/" + filename);
            data = IOUtils.toString(resource.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        T t = getObject(data, dataClass);
        return t;
    }

    public static <T> T getObject(String data, Class<T> clazz) {
        T t = null;
        if(clazz == String.class){
            return clazz.cast(data);
        }
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

            t = objectMapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return t;
    }
}

