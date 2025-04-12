package com.openclassrooms.paymybuddy.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.openclassrooms.paymybuddy.config.WebSecurityConfig;
import com.openclassrooms.paymybuddy.entity.Transaction;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import com.openclassrooms.paymybuddy.controller.controllerTestConfig.MockSecurityBeansConfig;

import com.openclassrooms.paymybuddy.controller.controllerTestConfig.TransactionControllerTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@WebMvcTest(controllers = TransactionController.class)
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebSecurityConfig.class, MockSecurityBeansConfig.class, TransactionControllerTestConfig.class})
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionServiceImpl transactionService;

    @WithMockUser(username = "john@gmail.com", roles = "USER")
    @Test
    void showTransactionPage_ShouldReturnTransactionView() throws Exception {

        User mockUser = new User();
        mockUser.setEmail("john@gmail.com");
        mockUser.setBalance(1000.0);

        // page de transactions
        Page<Transaction> mockPage = new PageImpl<>(List.of(
                new Transaction(1L, mockUser, mockUser, "testDescription", 100.0, 0.05, "EUR", LocalDateTime.now())
        ));

        when(transactionService.getUserByTransactionEmail("john@gmail.com")).thenReturn(mockUser);
        when(transactionService.getUserTransactionsPaginated(anyString(), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/transaction")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "createdAt")
                        .param("sortDirection", "desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("transaction"))
                .andExpect(model().attributeExists("transactions"));
    }

    @WithMockUser(username = "sender@gmail.com", roles = "USER")
    @Test
    void makeTransaction_ShouldRedirectWithSuccessMessage_WhenTransactionSucceeds() throws Exception {
        // Arrange
        Transaction mockTransaction = new Transaction();
        mockTransaction.setAmount(50.0);
        mockTransaction.setFee(2.5);
        mockTransaction.setCurrency("EUR");

        when(transactionService.makeTransaction(
                "sender@gmail.com",
                "receiver@gmail.com",
                50.0,
                "EUR",
                "Test payment"))
                .thenReturn(mockTransaction);

        // Act & Assert
        mockMvc.perform(post("/transaction")
                        .param("receiverEmail", "receiver@gmail.com")
                        .param("amount", "50.0")
                        .param("description", "Test payment")
                        .param("currency", "EUR"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transaction"))
                .andExpect(flash().attributeExists("success"))
                .andExpect(flash().attribute("success", "Transfert réussi: 50.00 EUR (frais: 2.50 EUR)"));
    }

    @WithMockUser(username = "sender@gmail.com", roles = "USER")
    @Test
    void makeTransaction_ShouldReturnError_WhenCurrencyNotSupported() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/transaction")
                        .header("Referer", "/transaction")
                        .param("receiverEmail", "receiver@gmail.com")
                        .param("amount", "50.0")
                        .param("description", "Test payment")
                        .param("currency", "XYZ"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/addRelationship"));

    }

}
