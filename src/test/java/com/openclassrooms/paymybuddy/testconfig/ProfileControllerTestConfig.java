package com.openclassrooms.paymybuddy.testconfig;
import com.openclassrooms.paymybuddy.service.UserService;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class ProfileControllerTestConfig {

    @Bean
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    public SecurityValidationImpl securityValidation() {
        return Mockito.mock(SecurityValidationImpl.class);
    }

}
