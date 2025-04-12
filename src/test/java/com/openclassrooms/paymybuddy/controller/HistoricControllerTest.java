package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.entity.Historic;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.service.serviceImpl.HistoricServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.openclassrooms.paymybuddy.config.WebSecurityConfig;

import com.openclassrooms.paymybuddy.testconfig.HistoricControllerTestConfig;
import com.openclassrooms.paymybuddy.testconfig.MockSecurityBeansConfig;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;


@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers = HistoricController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebSecurityConfig.class, MockSecurityBeansConfig.class, HistoricControllerTestConfig.class})
public class HistoricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricServiceImpl historicService;

    @BeforeEach
    void setUp() {
        Mockito.reset(historicService); // Réinitialisation des mocks
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void showHistoric_ShouldReturnHistoricView() throws Exception {
        // Arrange
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@gmail.com");

        Historic testHistoric = new Historic();
        Page<Historic> historicPage = new PageImpl<>(Collections.singletonList(testHistoric));

        when(historicService.getUserByEmail(anyString())).thenReturn(testUser);
        when(historicService.getUserHistoricPaginated(anyLong(), any(Pageable.class))).thenReturn(historicPage);

        // Act & Assert
        mockMvc.perform(get("/historic"))
                .andExpect(status().isOk())
                .andExpect(view().name("historic"))
                .andExpect(model().attributeExists("historics"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("currencySymbols"))
                .andExpect(model().attributeExists("userId"));

        verify(historicService).getUserByEmail("john@gmail.com");
        verify(historicService).getUserHistoricPaginated(1L, PageRequest.of(0, 10));
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void showHistoric_WithCustomPageSize_ShouldUseCorrectPagination() throws Exception {
        // Given
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@gmail.com");

        when(historicService.getUserByEmail("john@gmail.com")).thenReturn(testUser);
        when(historicService.getUserHistoricPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // When & Then
        mockMvc.perform(get("/historic?page=1&size=5"))
                .andExpect(status().isOk());

        // Vérification
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(historicService).getUserHistoricPaginated(eq(1L), pageableCaptor.capture());

        PageRequest capturedPageable = (PageRequest) pageableCaptor.getValue();
        assertAll(
                () -> assertEquals(1, capturedPageable.getPageNumber()),
                () -> assertEquals(5, capturedPageable.getPageSize())
        );
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void showHistoric_WhenUserNotFound_ShouldRedirectToErrorPage() throws Exception {
        // Arrange
        when(historicService.getUserByEmail(anyString())).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/historic"))
                .andExpect(status().is3xxRedirection()) // redirection (302)
                .andExpect(redirectedUrl("/error"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", "Une erreur système est survenue")); // Vérifie le message
    }

}
