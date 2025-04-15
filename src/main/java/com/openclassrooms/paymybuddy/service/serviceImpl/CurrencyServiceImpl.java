package com.openclassrooms.paymybuddy.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.openclassrooms.utils.CurrencySymbols.EXCHANGE_RATES;

@Service
@Slf4j
public class CurrencyServiceImpl {

    public double convertToEur(double amount, String fromCurrency) {

        if ("EUR".equalsIgnoreCase(fromCurrency)) {
            return amount;
        }

        Double rate = EXCHANGE_RATES.get(fromCurrency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Devise non supportée: " + fromCurrency);
        }

        log.debug("Conversion de {} {} en EUR", amount, fromCurrency);
        return amount * rate;
    }

    public double convertFromEur(double amount, String toCurrency) {

        if ("EUR".equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        Double rate = EXCHANGE_RATES.get(toCurrency.toUpperCase());
        if (rate == null) {
            throw new IllegalArgumentException("Devise non supportée: " + toCurrency);
        }

        //log.debug("Conversion de {} {} en EUR", amount, toCurrency);
        return amount / rate;
    }
}
