package com.openclassrooms.utils;

import java.util.Map;
import java.util.Set;

public class CurrencySymbols {

    public static final Map<String, Double> EXCHANGE_RATES = Map.of(
            "EUR", 1.0,    // Devise de référence
            "USD", 0.85,      // 1 USD = 0.85 EUR
            "XOF", 0.0015,    // 1 XOF = 0.0015 EUR
            "JPY", 0.0073,    // 1 JPY = 0.0073 EUR
            "CNY", 0.13,      // 1 CNY = 0.13 EUR
            "GBP", 1.1972,    // 1 GBP = 1.1972 EUR
            "RUB", 0.01095        // 1 RUB = 0.01095 EUR
    );

    public static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "XOF", "JPY", "CNY","RUB","GBP");

    public static final Map<String, String> SYMBOLS = Map.of(
            "EUR", "€",
            "USD", "$",
            "XOF", "CFA",
            "JPY", "¥",
            "CNY", "¥",
            "GBP", "£",
            "RUB", "₽"
    );
}
