package com.assignment.sm.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assignment.sm.domain.Currency;
import com.assignment.sm.domain.CurrencyPair;
import com.assignment.sm.exception.InvalidInputException;
import com.assignment.sm.repository.CurrencyPairRepository;
import com.assignment.sm.repository.CurrencyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CurrencyPairServiceTest {

  @InjectMocks
  CurrencyPairService currencyPairService;

  @Mock
  CurrencyRepository currencyRepository;

  @Mock
  CurrencyPairRepository currencyPairRepository;

  @Test
  public void testFindOrCreatePairRejectsSameCurrency(){
    Assertions.assertThrows(InvalidInputException.class, () -> {
      currencyPairService.findOrCreatePair("BTC", "btc");
    });
  }

  @Test
  public void testFindOrCreatePairReusesExistingPair(){
    CurrencyPair existingPair = new CurrencyPair(new Currency("Bitcoin", "BTC"), new Currency("United States Dollar", "USD"));
    when(currencyPairRepository.findByFromCurrency_AbbreviationAndToCurrency_Abbreviation("BTC", "USD"))
        .thenReturn(Optional.of(existingPair));

    CurrencyPair actualResult = currencyPairService.findOrCreatePair("BTC", "USD");

    Assertions.assertEquals(existingPair, actualResult);
    verify(currencyRepository, never()).save(any(Currency.class));
    verify(currencyPairRepository, never()).save(any(CurrencyPair.class));
  }

  @Test
  public void testFindOrCreatePairCreatesBothCurrenciesAndPairWhenMissing(){
    when(currencyPairRepository.findByFromCurrency_AbbreviationAndToCurrency_Abbreviation("BTC", "USD"))
        .thenReturn(Optional.empty());
    when(currencyRepository.findByAbbreviation("BTC")).thenReturn(null);
    when(currencyRepository.findByAbbreviation("USD")).thenReturn(null);
    when(currencyRepository.save(any(Currency.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(currencyPairRepository.save(any(CurrencyPair.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CurrencyPair actualResult = currencyPairService.findOrCreatePair("btc", "usd");

    Assertions.assertEquals("BTC", actualResult.getFromCurrency().getAbbreviation());
    Assertions.assertEquals("USD", actualResult.getToCurrency().getAbbreviation());
    verify(currencyRepository, times(2)).save(any(Currency.class));
    verify(currencyPairRepository, times(1)).save(any(CurrencyPair.class));
  }

  @Test
  public void testFindOrCreatePairReusesExistingCurrencyRows(){
    Currency existingFrom = new Currency("Bitcoin", "BTC");
    Currency existingTo = new Currency("United States Dollar", "USD");
    when(currencyPairRepository.findByFromCurrency_AbbreviationAndToCurrency_Abbreviation("BTC", "USD"))
        .thenReturn(Optional.empty());
    when(currencyRepository.findByAbbreviation("BTC")).thenReturn(existingFrom);
    when(currencyRepository.findByAbbreviation("USD")).thenReturn(existingTo);
    when(currencyPairRepository.save(any(CurrencyPair.class))).thenAnswer(invocation -> invocation.getArgument(0));

    CurrencyPair actualResult = currencyPairService.findOrCreatePair("BTC", "USD");

    Assertions.assertEquals(existingFrom, actualResult.getFromCurrency());
    Assertions.assertEquals(existingTo, actualResult.getToCurrency());
    verify(currencyRepository, never()).save(any(Currency.class));
  }
}
