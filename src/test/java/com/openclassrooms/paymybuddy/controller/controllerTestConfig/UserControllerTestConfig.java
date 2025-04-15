package com.openclassrooms.paymybuddy.controller.controllerTestConfig;

import com.openclassrooms.paymybuddy.service.serviceImpl.UserServiceImpl;
import com.openclassrooms.paymybuddy.validator.UserValidator;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class UserControllerTestConfig {

    @Bean
    public UserServiceImpl userService() {
        return Mockito.mock(UserServiceImpl.class);
    }

    @Bean
    public UserValidator userValidator() {
        return Mockito.mock(UserValidator.class);
    }
}
