package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.service.serviceImpl.CurrencyServiceImpl;
import com.openclassrooms.paymybuddy.service.serviceImpl.CustomUserDetailsService;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.InvalidPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class SecurityValidationImplTest {

    @Mock
    private CurrencyServiceImpl currencyService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private SecurityValidationImpl securityValidation;

    private User testUser;
    private User testReceiver;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("user@gmail.com");
        testUser.setBalance(100.0);

        testReceiver = new User();
        testReceiver.setEmail("receiver@gmail.com");
    }

    @Test
    void updateSecurityContext_shouldUpdateContextAndSession() {
        log.info("testUpdateSecurityContext_shouldUpdateContextAndSession");
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(customUserDetailsService.loadUserByUsername(testUser.getEmail())).thenReturn(userDetails);
        when(request.getSession(false)).thenReturn(session);

        // Act
        securityValidation.updateSecurityContext(testUser, request);

        // Assert
        verify(customUserDetailsService).loadUserByUsername(testUser.getEmail());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(session).setAttribute(
                eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY),
                eq(SecurityContextHolder.getContext()));
    }

    @Test
    void updateSecurityContext_shouldHandleNullSession() {
        log.info("testUpdateSecurityContext_shouldHandleNullSession");
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserByUsername(testUser.getEmail()))
                .thenReturn(userDetails);
        when(request.getSession(false)).thenReturn(null); // Simuler une session null

        // Act
        securityValidation.updateSecurityContext(testUser, request);

        // Assert
        verify(customUserDetailsService).loadUserByUsername(testUser.getEmail());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).getSession(false);
        verifyNoMoreInteractions(session); // Vérifier qu'on n'a pas interagi avec la session
    }

    @Test
    void validateTransaction_shouldNotThrowWhenValid() throws InsufficientBalanceException {
        log.info("testValidateTransaction_shouldNotThrowWhenValid");
        // Arrange
        testUser.getConnections().add(testReceiver);
        when(currencyService.convertToEur(100.0, "USD")).thenReturn(85.0);

        // Act & Assert
        assertDoesNotThrow(() ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, "USD"));
    }

    @Test
    void validateTransaction_shouldThrowWhenSenderEqualsReceiver() {
        log.info("testValidateTransaction_shouldThrowWhenSenderEqualsReceiver");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testUser, 100.0, "EUR"));
    }

    @Test
    void validateTransaction_shouldThrowWhenAmountNotPositive() {
        log.info("testValidateTransaction_shouldThrowWhenAmountNotPositive");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 0.0, "EUR"));
        assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, -100.0, "EUR"));
    }

    @Test
    void validateTransaction_shouldThrowWhenNotConnected() {
        log.info("testValidateTransaction_shouldThrowWhenNotConnected");
        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, "EUR"));
    }

    @Test
    void validateTransaction_shouldThrowWhenInsufficientBalance() {
        log.info("testValidateTransaction_shouldThrowWhenInsufficientBalance");
        // Arrange
        testUser.getConnections().add(testReceiver);
        when(currencyService.convertToEur(1000.0, "USD")).thenReturn(850.0);
        testUser.setBalance(100.0);

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 1000.0, "USD"));

        assertTrue(exception.getMessage().contains("Solde insuffisant"));
    }

    @Test
    void validateTransaction_shouldThrowWhenSenderIsNull() {
        log.info("testValidateTransaction_shouldThrowWhenSenderIsNull");
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(null, testReceiver, 100.0, "EUR"));
        assertEquals("L'expéditeur et le destinataire doivent être spécifiés", exception.getMessage());
    }

    @Test
    void validateTransaction_shouldThrowWhenReceiverIsNull() {
        log.info("testValidateTransaction_shouldThrowWhenReceiverIsNull");
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, null, 100.0, "EUR"));
        assertEquals("L'expéditeur et le destinataire doivent être spécifiés", exception.getMessage());
    }

    @Test
    void validateTransaction_shouldThrowWhenCurrencyIsNull() {
        log.info("testValidateTransaction_shouldThrowWhenCurrencyIsNull");

        // Arrange
        testUser.getConnections().add(testReceiver);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, null));
        assertEquals("Devise invalide", exception.getMessage());
    }

    @Test
    void validateTransaction_shouldThrowWhenCurrencyLengthNot3() {
        log.info("testValidateTransaction_shouldThrowWhenCurrencyLengthNot3");
        // Arrange
        testUser.getConnections().add(testReceiver);

        // Test avec devise trop courte
        assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, "EU"));

        // Test avec devise trop longue
        assertThrows(IllegalArgumentException.class, () ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, "EURO"));
    }

    @Test
    void validateTransaction_shouldAcceptValid3LetterCurrency() throws InsufficientBalanceException {
        log.info("testValidateTransaction_shouldAcceptValid3LetterCurrency");
        // Arrange
        testUser.getConnections().add(testReceiver);
        when(currencyService.convertToEur(100.0, "USD")).thenReturn(85.0);

        // Act & Assert
        assertDoesNotThrow(() ->
                securityValidation.validateTransaction(testUser, testReceiver, 100.0, "USD"));
    }

    @Test
    void validateEmail_shouldNotThrowWhenValid() {
        log.info("testValidateEmail_shouldNotThrowWhenValid");
        // Act & Assert
        assertDoesNotThrow(() -> securityValidation.validateEmail("valid@email.com"));
    }

    @Test
    void validateEmail_shouldThrowWhenInvalid() {
        log.info("testValidateEmail_shouldThrowWhenInvalid");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> securityValidation.validateEmail("invalid-email"));
    }

    @Test
    void validatePassword_shouldNotThrowWhenValid() throws InvalidPasswordException {
        log.info("testValidatePassword_shouldNotThrowWhenValid");
        // Act & Assert
        assertDoesNotThrow(() -> securityValidation.validatePassword("Valid1@Password"));
    }

    @Test
    void validatePassword_shouldThrowWhenTooShort() {
        log.info("testValidatePassword_shouldThrowWhenTooShort");
        assertThrows(InvalidPasswordException.class, () -> securityValidation.validatePassword("Short1@"));
    }

    @Test
    void validatePassword_shouldThrowWhenNoSpecialChar() {
        log.info("testValidatePassword_shouldThrowWhenNoSpecialChar");
        assertThrows(InvalidPasswordException.class, () -> securityValidation.validatePassword("NoSpecial1"));
    }

    @Test
    void validatePassword_shouldThrowWhenNoDigit() {
        log.info("testValidatePassword_shouldThrowWhenNoDigit");
        assertThrows(InvalidPasswordException.class, () -> securityValidation.validatePassword("NoDigit@"));
    }

    @Test
    void validatePassword_shouldThrowWhenNoUpperCase() {
        log.info("testValidatePassword_shouldThrowWhenNoUpperCase");
        assertThrows(InvalidPasswordException.class, () -> securityValidation.validatePassword("nouppercase1@"));
    }

    @Test
    void validatePassword_shouldThrowWhenNoLowerCase() {
        log.info("testValidatePassword_shouldThrowWhenNoLowerCase");
        assertThrows(InvalidPasswordException.class, () -> securityValidation.validatePassword("NOLOWERCASE1@"));
    }
}