package com.openclassrooms.paymybuddy.controller.controllerTestConfig;

import com.openclassrooms.paymybuddy.service.UserService;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserConnectionServiceImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class ConnectionControllerTestConfig {

    @Bean
    private UserConnectionServiceImpl connectionService() {
        return Mockito.mock(UserConnectionServiceImpl.class);
    }

    @Bean
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }


}
