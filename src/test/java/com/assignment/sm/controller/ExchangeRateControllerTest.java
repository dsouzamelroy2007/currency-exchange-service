package com.assignment.sm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.exception.ExchangeRateFetchException;
import com.assignment.sm.exception.ExchangeRateParseException;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.model.CurrencyExchangeRate;
import com.assignment.sm.model.ExchangeRateResponse;
import com.assignment.sm.service.ExchangeRateService;
import com.assignment.sm.util.MockDataCreator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith(SpringExtension.class)
@WebMvcTest
@ContextConfiguration(classes = ExchangeRateController.class)
@AutoConfigureMockMvc
public class ExchangeRateControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  private ExchangeRateService exchangeRateService;

  private CurrencyExchangeRate exchangeRate;

  private List<CurrencyExchangeRate> historicalExchangeRates;

  @BeforeEach
  public void setUp(){
    exchangeRate = MockDataCreator.currencyExchangeLiveRate();
    Currency currency = MockDataCreator.getCurrencyObjectForTest();
    historicalExchangeRates = MockDataCreator.currencyExchangeHistoricalRates(currency);
  }

  @Test
  public void testGetLiveRateis404() throws Exception{
    when(exchangeRateService.getCurrencyExchangeRate())
                .thenReturn(exchangeRate);

    this.mockMvc.perform(get("/exchange1/liveRate")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andReturn();
  }

  @Test
  public void testGetLiveRateis500() throws Exception{
    ExchangeRateResponse response = MockDataCreator.getResponseForLiveRate();
    when(exchangeRateService.getCurrencyExchangeRate())
                .thenThrow(new ExchangeRateFetchException("Dummy exception"));

    this.mockMvc.perform(get("/exchange/liveRate"))
                .andExpect(status().isInternalServerError());
  }

  @Test
  public void testGetLiveRateisOk() throws Exception {
    ExchangeRateResponse response = MockDataCreator.getResponseForLiveRate();
    when(exchangeRateService.getCurrencyExchangeRate())
                .thenReturn(exchangeRate);

    this.mockMvc.perform(get("/exchange/liveRate"))
                .andExpect(status().isOk())
                .andReturn();

  }


  @Test
  public void testGetHistoricalRatesis404() throws Exception{
    when(exchangeRateService.getHistoricalExchangeRates(anyString(), any(LocalDate.class), any(LocalDate.class)))
               .thenReturn(historicalExchangeRates);

    MvcResult mvcResult = this.mockMvc.perform(get("/exchange/historicalRate")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andReturn();

  }

  @Test
  public void testGetHistoricalRatesis500() throws Exception{
    ExchangeRateResponse response = MockDataCreator.getResponseForHistoricalRates();
    when(exchangeRateService.getHistoricalExchangeRates(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new ExchangeRateParseException("Dummy exception"));

   this.mockMvc.perform(get("/exchange/historicalRate?startDate=2021-01-20&endDate=2021-03-11"))
                .andExpect(status().isInternalServerError());
  }

  @Test
  public void testGetHistoricalRatesisis400() throws Exception{
    ExchangeRateResponse response = MockDataCreator.getResponseForHistoricalRates();
    when(exchangeRateService.getHistoricalExchangeRates(anyString(), any(LocalDate.class), any(LocalDate.class)))
        .thenThrow(new InvalidInputException("Invalid Dates"));

    this.mockMvc.perform(get("/exchange/historicalRate?startDate=2021-01-20&endDate=2021-01-11"))
        .andExpect(status().isBadRequest());
  }


  @Test
  public void testGetHistoricalRatesisOk() throws Exception {
    ExchangeRateResponse response = MockDataCreator.getResponseForLiveRate();
    when(exchangeRateService.getHistoricalExchangeRates(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(historicalExchangeRates);

    this.mockMvc.perform(get("/exchange/historicalRate?startDate=2021-01-20&endDate=2021-03-11"))
                .andExpect(status().isOk());

  }
}
