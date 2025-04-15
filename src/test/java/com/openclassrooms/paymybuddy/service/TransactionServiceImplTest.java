package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.entity.Transaction;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.serviceImpl.CurrencyServiceImpl;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrencyServiceImpl currencyService;

    @Mock
    private SecurityValidationImpl securityValidation;

    @Mock
    private Page<Transaction> transactionPage;

    @Mock
    private Pageable pageable;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setEmail("sender@gmail.com");
        sender.setBalance(1000.0);

        receiver = new User();
        receiver.setEmail("receiver@gmail.com");
        receiver.setBalance(500.0);

    }

    @Test
    void getUserTransactionsPaginated_shouldReturnPage() {
        log.info("getUserTransactionsPaginated_shouldReturnPage");
        when(transactionRepository.findBySenderEmailOrReceiverEmail(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(transactionPage);

        Page<Transaction> result = transactionService.getUserTransactionsPaginated("john@gmail.com", pageable);

        assertNotNull(result);
        verify(transactionRepository).findBySenderEmailOrReceiverEmail("john@gmail.com", "john@gmail.com", pageable);
    }

    @Test
    void getUserWithConnections_shouldReturnUser() throws UserNotFoundException {
        log.info("getUserWithConnections_shouldReturnUser");
        when(userRepository.findWithConnectionsByEmail("userconnection@gmail.com")).thenReturn(Optional.of(sender));

        User result = transactionService.getUserWithConnections("userconnection@gmail.com");

        assertNotNull(result);
        assertEquals("sender@gmail.com", result.getEmail());
    }

    @Test
    void getUserWithConnections_shouldThrowWhenUserNotFound() {
        log.info("getUserWithConnections_shouldThrowWhenUserNotFound");
        when(userRepository.findWithConnectionsByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> transactionService.getUserWithConnections("unknown@test.com"));
    }

    @Test
    void getUserByTransactionEmail_shouldReturnUser() throws UserNotFoundException {
        log.info("getUserByTransactionEmail_shouldReturnUser");
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(sender));
        User result = transactionService.getUserByTransactionEmail("john@gmail.com");
        assertNotNull(result);
        assertEquals("sender@gmail.com", result.getEmail());
    }

    @Test
    void getUserByTransactionEmail_shouldThrowWhenUserNotFound() {
        log.info("getUserByTransactionEmail_shouldThrowWhenUserNotFound");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> transactionService.getUserByTransactionEmail("unknown@test.com"));
    }

    @Test
    void addBalance_shouldAddSpecifiedAmount() throws UserNotFoundException {
        log.info("addBalance_shouldAddSpecifiedAmount");
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(sender));
        when(userRepository.save(any(User.class))).thenReturn(sender);

        User result = transactionService.addBalance("john@gmail.com", 100.0, null);

        assertEquals(1100.0, result.getBalance(), 0.001);
        verify(userRepository).save(sender);
    }

    @Test
    void addBalance_shouldThrowWhenEmailBlank() {
        log.info("addBalance_shouldThrowWhenEmailBlank");
        assertThrows(IllegalArgumentException.class, () -> transactionService.addBalance("", 100.0, null));
    }

    @Test
    void addBalance_shouldThrowWhenNoAmountSpecified() {
        log.info("addBalance_shouldThrowWhenNoAmountSpecified");
        assertThrows(IllegalArgumentException.class, () -> transactionService.addBalance("john@gmail.com", null, null));
    }

    @Test
    void addBalance_shouldThrowWhenEmailNull() {
        log.info("addBalance_shouldThrowWhenEmailNull");
        assertThrows(IllegalArgumentException.class, () -> transactionService.addBalance(null, 100.0, null));
    }

    @Test
    void getFormattedBalanceUpdateMessage_shouldFormatMessageCorrectly() {
        log.info("getFormattedBalanceUpdateMessage_shouldFormatMessageCorrectly");
        User user = new User();
        user.setBalance(1500.0);
        user.setTemporaryAmountAdded(500.0);

        String message = transactionService.getFormattedBalanceUpdateMessage(user, null);
        assertTrue(message.contains("500.00 € ajoutés"));
        assertTrue(message.contains("1500.00 €"));

        message = transactionService.getFormattedBalanceUpdateMessage(user, 300.0);
        assertTrue(message.contains("300.00 € ajoutés"));
        assertTrue(message.contains("1500.00 €"));
    }

    @Test
    void makeTransaction_shouldProcessValidTransaction() throws Exception {
        log.info("makeTransaction_shouldProcessValidTransaction");

        // Arrange
        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail("receiver@gmail.com")).thenReturn(Optional.of(receiver));
        when(currencyService.convertToEur(100.0, "USD")).thenReturn(85.0);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        sender.getConnections().add(receiver);

        // Act
        Transaction transaction = transactionService.makeTransaction(
                "sender@gmail.com", "receiver@gmail.com",
                100.0, "USD", "Test payment");

        // Assert
        assertNotNull(transaction);
        assertEquals(100.0, transaction.getAmount());
        assertEquals(85.0 * 0.005, transaction.getFee(), 0.001);
        assertEquals("USD", transaction.getCurrency());
        assertEquals("Test payment", transaction.getDescription());

        // Verify balances
        assertEquals(1000.0 - (85.0 + 85.0 * 0.005), sender.getBalance(), 0.001);
        assertEquals(500.0 + 85.0, receiver.getBalance(), 0.001);

        verify(securityValidation).validateTransaction(sender, receiver, 100.0, "USD");
        verify(userRepository).saveAll(List.of(sender, receiver));
    }

    @Test
    void makeTransaction_shouldThrowWhenSenderNotFound() {
        log.info("makeTransaction_shouldThrowWhenSenderNotFound");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                transactionService.makeTransaction(
                        "unknown@test.com", "receiver@gmail.com",
                        100.0, "EUR", "Test"));
    }

    @Test
    void makeTransaction_shouldThrowWhenReceiverNotFound() {
        log.info("makeTransaction_shouldThrowWhenReceiverNotFound");
        when(userRepository.findByEmail("sender@gmail.com")).thenReturn(Optional.of(sender));

        assertThrows(UserNotFoundException.class, () ->
                transactionService.makeTransaction(
                        "sender@gmail.com", "unknown@test.com",
                        100.0, "EUR", "Test"));
    }

    @Test
    void calculateAmountToAdd_shouldThrowWhenInvalidAmounts() {
        log.info("calculateAmountToAdd_shouldThrowWhenInvalidAmounts");
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.calculateAmountToAdd(-100.0, null));
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.calculateAmountToAdd(null, "invalid"));
    }
}
