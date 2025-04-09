package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.service.serviceImpl.CurrencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import lombok.extern.slf4j.Slf4j;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.openclassrooms.utils.CurrencySymbols.EXCHANGE_RATES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class CurrencyServiceImplTest {

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    @BeforeEach
    void setUp() {}

    @Test
    void convertToEur_shouldReturnSameAmountWhenFromEUR() {
        log.info("convertToEur_shouldReturnSameAmountWhenFromEUR");
        // Arrange
        double amount = 100.0;
        String currency = "EUR";
        // Act
        double result = currencyService.convertToEur(amount, currency);
        // Assert
        assertEquals(amount, result);
    }

    @Test
    void convertToEur_shouldConvertCorrectlyWhenFromUSD() {
        log.info("convertToEur_shouldConvertCorrectlyWhenFromUSD");
        // Arrange
        double amount = 100.0;
        String currency = "USD";
        double expected = amount * EXCHANGE_RATES.get("USD");
        // Act
        double result = currencyService.convertToEur(amount, currency);
        // Assert
        assertEquals(expected, result);
    }

    @Test
    void convertToEur_shouldThrowExceptionWhenCurrencyNotSupported() {
        log.info("convertToEur_shouldThrowExceptionWhenCurrencyNotSupported");
        // arrange
        double amount = 100.0;
        String currency = "KLM";
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            currencyService.convertToEur(amount, currency));
        // Assert
        assertEquals("Devise non supportée: " + currency, exception.getMessage());
    }

    @Test
    void convertFromEur_shouldReturnSameAmountWhenToEUR() {
        log.info("convertFromEur_shouldReturnSameAmountWhenToEUR");
        // Arrange
        double amount = 100.0;
        String currency = "EUR";
        // Assert
        double result = currencyService.convertFromEur(amount, currency);
        // Act
        assertEquals(amount, result);
    }

    @Test
    void convertFromEur_shouldConvertCorrectlyWhenToUSD() {
        log.info("convertFromEur_shouldConvertCorrectlyWhenToUSD");
        // Assert
        double amount = 100.0;
        String currency = "USD";
        double expected = amount / EXCHANGE_RATES.get("USD");
        // Act
        double result = currencyService.convertFromEur(amount, currency);
        // Assert
        assertEquals(expected, result);
    }

    @Test
    void convertFromEur_shouldThrowExceptionWhenCurrencyNotSupported() {
        log.info("convertFromEur_shouldThrowExceptionWhenCurrencyNotSupported");
        // Arrange
        double amount = 100.0;
        String currency = "KLM";
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            currencyService.convertFromEur(amount, currency));
        // Assert
        assertEquals("Devise non supportée: " + currency, exception.getMessage());
    }

}
