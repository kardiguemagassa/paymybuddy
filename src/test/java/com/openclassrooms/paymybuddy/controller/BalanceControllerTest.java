package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.testconfig.BalanceControllerTestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;


import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers = BalanceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({BalanceControllerTestConfig.class})
public class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionServiceImpl transactionService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        // Initialiser un utilisateur mocké pour le test
        mockUser = new User();
        mockUser.setEmail("test@mail.com");
        mockUser.setBalance(100.0);
    }

    @Test
    @WithMockUser(username = "test@mail.com", roles = "USER")
    void showAddBalancePage_ShouldReturnBalancePage_WhenUserIsAuthenticated() throws Exception {
        // Mock du service pour renvoyer un utilisateur
        when(transactionService.getUserByTransactionEmail("test@mail.com")).thenReturn(mockUser);

        // Tester le GET sur /addBalance
        mockMvc.perform(MockMvcRequestBuilders.get("/addBalance"))
                .andExpect(status().isOk()) // Vérifie que la page se charge correctement
                .andExpect(view().name("balance")) // Vérifie que la vue rendue est "balance"
                .andExpect(model().attributeExists("user", "currentBalance")) // Vérifie que les attributs sont bien présents dans le modèle
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attribute("currentBalance", 100.0)); // Vérifie que le solde est correct
    }

    @Test
    @WithMockUser(username = "test@mail.com", roles = "USER")
    void addBalance_ShouldRedirectToBalancePage_WhenBalanceIsAdded() throws Exception {
        double amountToAdd = 50.0;
        String successMessage = "Votre solde a été mis à jour de " + amountToAdd + "€.";

        // Mock de l'ajout de solde et du message de succès
        when(transactionService.addBalance("test@mail.com", amountToAdd, null)).thenReturn(mockUser);
        when(transactionService.getFormattedBalanceUpdateMessage(mockUser, amountToAdd)).thenReturn(successMessage);

        // Tester le POST sur /addBalance avec un montant
        mockMvc.perform(MockMvcRequestBuilders.post("/addBalance")
                        .param("amount", String.valueOf(amountToAdd)))
                .andExpect(status().is3xxRedirection()) // Vérifie la redirection
                .andExpect(redirectedUrlPattern("/addBalance?success*")); // Vérifie que l'URL de redirection contient le paramètre "success"

    }


    @Test
    @WithMockUser(username = "test@mail.com", roles = "USER")
    void addBalance_ShouldRedirectToBalancePage_WhenRandomAmountIsAdded() throws Exception {
        String randomAmount = "randomAmount"; // Mock un randomAmount pour la validation
        String successMessage = "Votre solde a été mis à jour.";

        // Mock du service pour l'ajout de solde avec randomAmount
        when(transactionService.addBalance("test@mail.com", null, randomAmount)).thenReturn(mockUser);
        when(transactionService.getFormattedBalanceUpdateMessage(mockUser, null)).thenReturn(successMessage);

        // Tester le POST sur /addBalance avec randomAmount
        mockMvc.perform(MockMvcRequestBuilders.post("/addBalance")
                        .param("randomAmount", randomAmount))
                .andExpect(status().is3xxRedirection()) // Vérifie la redirection
                .andExpect(redirectedUrlPattern("/addBalance?success*")); // Vérifie que l'URL de redirection est correcte
    }
}
