package com.openclassrooms.paymybuddy.testconfig;

import com.openclassrooms.paymybuddy.service.serviceImpl.HistoricServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class HistoricControllerTestConfig {

    @Bean
    public HistoricServiceImpl historicService() {
        return Mockito.mock(HistoricServiceImpl.class);
    }
}
