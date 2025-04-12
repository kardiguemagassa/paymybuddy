package com.openclassrooms.paymybuddy.testconfig;

import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class BalanceControllerTestConfig {

    @Bean
    public TransactionServiceImpl transactionService() {
        return Mockito.mock(TransactionServiceImpl.class);
    }
}
