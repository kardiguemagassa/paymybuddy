package com.openclassrooms.paymybuddy.controller.controllerTestConfig;

import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class TransactionControllerTestConfig {

    @Bean
    public TransactionServiceImpl transactionService() {
        return Mockito.mock(TransactionServiceImpl.class);
    }
}
