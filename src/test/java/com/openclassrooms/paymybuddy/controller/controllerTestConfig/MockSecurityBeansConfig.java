package com.openclassrooms.paymybuddy.controller.controllerTestConfig;

import com.openclassrooms.paymybuddy.config.CustomAuthenticationSuccessHandler;
import com.openclassrooms.paymybuddy.service.serviceImpl.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;


@Configuration
@Slf4j
public class MockSecurityBeansConfig {

    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        log.debug("mock CustomUserDetailsService");
        CustomUserDetailsService mockCustomUserDetailsService = mock(CustomUserDetailsService.class);

        //  éviter le NullPointerException
        doAnswer(invocation -> {
            String username = invocation.getArgument(0);
            return new org.springframework.security.core.userdetails.User(
                    username, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        }).when(mockCustomUserDetailsService).loadUserByUsername(any());

        return mockCustomUserDetailsService;
    }

    @Bean
    @Primary
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() throws ServletException, IOException {
        CustomAuthenticationSuccessHandler mock = mock(CustomAuthenticationSuccessHandler.class);

        doAnswer(inv -> {
            ((HttpServletResponse) inv.getArgument(1)).sendRedirect("/default-success");
            //((HttpServletResponse) inv.getArgument(1)).sendRedirect("/home");
            return null;
        }).when(mock).onAuthenticationSuccess(any(), any(), any());

        return mock;
    }

}
