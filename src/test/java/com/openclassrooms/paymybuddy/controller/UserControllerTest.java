package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.config.WebSecurityConfig;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserServiceImpl;
import com.openclassrooms.paymybuddy.testconfig.MockSecurityBeansConfig;
import com.openclassrooms.paymybuddy.testconfig.UserControllerTestConfig;
import com.openclassrooms.paymybuddy.validator.UserValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.WebApplicationContext;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebSecurityConfig.class, MockSecurityBeansConfig.class, UserControllerTestConfig.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private UserValidator userValidator;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {

        reset(userService, userValidator);
        doNothing().when(userValidator).validate(any(), any());
        when(userValidator.supports(User.class)).thenReturn(true);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Configuration du validateur
        doAnswer(invocation -> {
            BindingResult bindingResult = invocation.getArgument(1);
            // Ne pas ajouter d'erreurs par défaut
            return null;
        }).when(userValidator).validate(any(), any());

    }

    @Test
    void showRegistrationForm_ShouldWork() throws Exception {
        mockMvc.perform(get("/register")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldRedirectToRegister() throws Exception {
        when(userService.findUserByEmail(anyString())).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "john")
                        .param("email", "johnexisting@gmail.com")
                        .param("password", "John@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"));
    }

    @Test
    void registerUser_WithValidData_ShouldRedirectToLogin() throws Exception {
        // Arrange
        when(userService.findUserByEmail("john@gmail.com")).thenReturn(Optional.empty());
        doNothing().when(userService).registerUser(any(User.class));

        // Act & Assert
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "John Doe")
                        .param("username", "john")
                        .param("email", "john@gmail.com")
                        .param("password", "John@123"))
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/login"),
                        flash().attribute("success", "Inscription réussie !")
                );

        // Verify
        verify(userService).registerUser(argThat(user -> user.getEmail().equals("john@gmail.com")));
        verify(userValidator, atLeastOnce()).validate(any(), any());
    }

    @Test
    void registerUser_WithInvalidData_ShouldPreserveInputValues() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "J")  // Trop court
                        .param("email", "bad-email")
                        .param("password", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("user", hasProperty("email", equalTo("bad-email"))));
    }

    @Test
    void showLoginForm_ShouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeDoesNotExist("success"));
    }

    @ParameterizedTest
    @MethodSource("loginStatusProvider")
    void showLoginForm_WithStatusParameters_ShouldAddModelAttributes(
            String paramName, String paramValue, String attributeName, String expectedMessage) throws Exception {

        mockMvc.perform(get("/login").param(paramName, paramValue))
                .andExpect(status().isOk())
                .andExpect(model().attribute(attributeName, expectedMessage));
    }

    private static Stream<Arguments> loginStatusProvider() {
        return Stream.of(
                Arguments.of("error", "true", "error", "Email ou mot de passe incorrect"),
                Arguments.of("logout", "true", "success", "Vous avez été déconnecté avec succès"),
                Arguments.of("registered", "true", "success", "Inscription réussie ! Vous pouvez maintenant vous connecter"));
    }

    @Test
    void logout_ShouldClearSecurityContextAndRedirect() throws Exception {
        // Given
        Authentication auth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When
        MvcResult result = mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // vérifier que la session est invalidée
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void logout_WhenNotAuthenticated_ShouldStillRedirect() throws Exception {
        // qu'aucune authentification n'est active
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}