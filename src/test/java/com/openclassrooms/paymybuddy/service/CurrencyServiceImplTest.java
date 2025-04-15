package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.service.serviceImpl.CurrencyServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class CurrencyServiceImplTest {

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    @BeforeEach
    void setUp() {
        currencyService = new CurrencyServiceImpl();
    }

    @Test
    void convertToEur_shouldReturnSameAmountIfCurrencyIsEur() {
        log.info("convertToEur shouldReturnSameAmountIfCurrencyIsEur");
        double result = currencyService.convertToEur(100.0, "EUR");
        assertEquals(100.0, result);
    }

    @Test
    void convertToEur_shouldConvertUsdToEur() {
        log.info("convertToEur shouldConvertUsdToEur");
        double result = currencyService.convertToEur(100.0, "USD");
        // Exemple: si 1 USD = 0.85 EUR
        double expected = 100.0 * 0.85; // selon la valeur réelle dans EXCHANGE_RATES
        assertEquals(expected, result, 0.0001);
    }

    @Test
    void convertToEur_shouldThrowExceptionForUnknownCurrency() {
        log.info("convertToEur shouldThrowExceptionForUnknownCurrency");
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                currencyService.convertToEur(100.0, "XYZ"));
        assertTrue(exception.getMessage().contains("Devise non supportée"));
    }

    @Test
    void convertFromEur_shouldReturnSameAmountIfCurrencyIsEur() {
        log.info("convertFromEur shouldReturnSameAmountIfCurrencyIsEur");
        double result = currencyService.convertFromEur(100.0, "EUR");
        assertEquals(100.0, result);
    }

    @Test
    void convertFromEur_shouldConvertEurToUsd() {
        log.info("convertFromEur shouldConvertEurToUsd");
        double result = currencyService.convertFromEur(85.0, "USD");
        // Exemple : si 1 USD = 0.85 EUR → 1 EUR = 1/0.85 USD
        double expected = 85.0 / 0.85; // selon la valeur réelle dans EXCHANGE_RATES
        assertEquals(expected, result, 0.0001);
    }

    @Test
    void convertFromEur_shouldThrowExceptionForUnknownCurrency() {
        log.info("convertFromEur shouldThrowExceptionForUnknownCurrency");
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                currencyService.convertFromEur(100.0, "ABC"));
        assertTrue(exception.getMessage().contains("Devise non supportée"));
    }
}
