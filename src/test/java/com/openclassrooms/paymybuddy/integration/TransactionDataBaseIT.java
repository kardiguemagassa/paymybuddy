package com.openclassrooms.paymybuddy.integration;

import com.openclassrooms.paymybuddy.controller.controllerTestConfig.MockSecurityBeansConfig;
import com.openclassrooms.paymybuddy.entity.Transaction;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(MockSecurityBeansConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TransactionDataBaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private User sender;
    private User receiver;

    @BeforeEach
    public void setUp() throws Exception {
        userRepository.deleteAll();

        sender = new User();
        sender.setEmail("sender@gmail.com");
        sender.setPassword(new BCryptPasswordEncoder().encode("Sender#123"));
        sender.setName("Sender");
        sender.setBalance(1000.0);

        receiver = new User();
        receiver.setEmail("receiver@gmail.com");
        receiver.setPassword(new BCryptPasswordEncoder().encode("Receiver#123"));
        receiver.setName("Receiver");
        receiver.setBalance(500.0);

        userRepository.save(receiver);
        sender.addConnection(receiver);
        userRepository.save(sender);

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(50.0);
        transaction.setFee(0.5);
        transaction.setCurrency("EUR");
        transaction.setDescription("Test Transaction");

        transactionRepository.save(transaction);
    }

    @Test
    public void shouldConnectToTestDatabase() {
        long count = userRepository.count();
        assertTrue(count > 0);
    }

    @Test
    @WithMockUser(username = "sender@gmail.com")
    public void transactionPage_ShouldContainExpectedTransaction() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(view().name("transaction"))
                .andExpect(model().attributeExists("transactions"))
                .andDo(print())
                .andExpect(model().attribute("transactions", hasItem(
                        allOf(
                                hasProperty("amount", is(50.0)),
                                hasProperty("currency", is("EUR")),
                                hasProperty("description", is("Test Transaction"))
                        )
                )));
    }


    @Test
    @WithMockUser(username = "sender@gmail.com")
    public void showTransactionPage_ShouldDisplayCorrectBalance() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(model().attribute("currentBalance", 1000.0));
    }

    @Test
    @WithMockUser(username = "sender@gmail.com")
    public void makeTransaction_ShouldCreateTransactionRecord() throws Exception {
        mockMvc.perform(post("/transaction")
                        .param("receiverEmail", "receiver@gmail.com")
                        .param("amount", "20.0")
                        .param("description", "Check DB Insert")
                        .param("currency", "EUR")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection());

        List<Transaction> transactions = transactionRepository.findAll();
        assertTrue(transactions.stream()
                .anyMatch(t -> t.getDescription().equals("Check DB Insert") && t.getAmount() == 20.0));
    }


    @Test
    @WithMockUser(username = "sender@gmail.com")
    public void makeTransaction_WithNonexistentUser_ShouldFail() throws Exception {
        mockMvc.perform(post("/transaction")
                        .param("receiverEmail", "nonexistent@example.com")
                        .param("amount", "50.0")
                        .param("description", "Should fail")
                        .param("currency", "EUR")
                        .header("Referer", "/transaction") // <-- C'est la clé m'a sauvé la vie!
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transaction"))
                .andExpect(flash().attributeExists("errorTransaction"))
                .andExpect(flash().attribute("errorTransaction", "Destinataire non trouvé"));
    }
}