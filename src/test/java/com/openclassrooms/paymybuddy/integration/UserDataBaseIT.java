package com.openclassrooms.paymybuddy.integration;

import com.openclassrooms.paymybuddy.controller.controllerTestConfig.MockSecurityBeansConfig;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;


//@Transactional // nettoie les données entre les tests
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Pour éviter de réinitialiser le contexte à chaque test (optionnel)
@Import({MockSecurityBeansConfig.class}) //  injecter des beans mockés
@AutoConfigureMockMvc // Injecte MockMvc pour tester les endpoints HTTP
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest // Démarre tout le contexte Spring (comme en production)
public class UserDataBaseIT {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données avant chaque test
        userRepository.deleteAll();

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setEmail("john@gmail.com");
        testUser.setPassword(new BCryptPasswordEncoder().encode("John#123"));
        testUser.setName("John");
        userRepository.save(testUser);
    }

    @Test
    void shouldConnectToTestDatabase() {
        long count = userRepository.count();
        assertTrue(count >= 0); // Juste pour valider la connexion à la BDD
    }

    @Test
    void testRegistrationWorkflow() throws Exception {

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        mockMvc.perform(post("/register")
                        .param("name", "Jane")
                        .param("email", "jane@gmail.com")
                        .param("password", "Jane123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        // Vérifier user a bien été créé
        assertTrue(userRepository.findByEmail("jane@gmail.com").isPresent());
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void testAuthenticationWorkflow() throws Exception {

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void testRegistrationWithExistingEmail() throws Exception {

        //avec un email existant
        mockMvc.perform(post("/register")
                        .param("name", testUser.getName())
                        .param("email", testUser.getEmail()) // Email déjà existant
                        .param("password", testUser.getPassword())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.user"));
    }

    @Test
    void testInvalidRegistrationData() throws Exception {
        // Données invalides (nom trop court, email invalide, mot de passe faible)
        mockMvc.perform(post("/register")
                        .param("name", "John")
                        .param("email", "john@gmail.come")
                        .param("password", "john")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("errors"))
                .andExpect(flash().attributeExists("user"));

    }

    @Test
    void testLoginWithErrorParameter() throws Exception {
        // Simuler une tentative de connexion échouée
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Email ou mot de passe incorrect"));
    }

    @Test
    void testLoginAfterRegistration() throws Exception {
        // Simuler une redirection après inscription réussie
        mockMvc.perform(get("/login").param("registered", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("success", "Inscription réussie ! Vous pouvez maintenant vous connecter"));
    }
}
