package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.controller.controllerTestConfig.ConnectionControllerTestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.UserService;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserConnectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers = ConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ConnectionControllerTestConfig.class})
public class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserConnectionServiceImpl connectionService;

    @Autowired
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@gmail.com");

        // connexions user sous forme d'un Set d'User
        Set<User> userConnections = new HashSet<>();
        User contact1 = new User();
        contact1.setEmail("contact1@gmail.com");
        userConnections.add(contact1);

        User contact2 = new User();
        contact2.setEmail("contact2@gmail.com");
        userConnections.add(contact2);

        // connexions potentielles sous forme d'un Set d'User
        Set<User> potentialConnections = new HashSet<>();
        User newContact = new User();
        newContact.setEmail("newContact@mail.com");
        potentialConnections.add(newContact);

        when(userService.getUserByEmail(anyString())).thenReturn(mockUser);
        when(connectionService.getUserConnections(anyString())).thenReturn(userConnections);
        when(connectionService.getPotentialConnections(anyString())).thenReturn(potentialConnections);
    }

    @Test
    @WithMockUser(username = "john@gmail.com", roles = "USER")
    void showConnectionsPage_ShouldReturnViewWithAttributes() throws Exception {
        mockMvc.perform(get("/addRelationship"))
                .andExpect(status().isOk())
                .andExpect(view().name("addRelationship"))
                .andExpect(model().attributeExists("relations", "potentialConnections", "user"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com", roles = "USER")
    void addConnection_ShouldRedirectWithSuccessMessage() throws Exception {
        doNothing().when(connectionService).addConnection(anyString(), anyString());

        mockMvc.perform(post("/addRelationship/add")
                        .param("email", "newContact@mail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/addRelationship"))
                .andExpect(flash().attribute("relationSuccessMessage", "newContact@mail.com est ajouté avec succès"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com", roles = "USER")
    void updateConnection_ShouldRedirectWithSuccessMessage() throws Exception {
        doNothing().when(connectionService).updateConnection(anyString(), anyString(), anyString());

        mockMvc.perform(post("/addRelationship/update")
                        .param("oldConnectionEmail", "contact1@mail.com")
                        .param("newConnectionEmail", "updatedContact@mail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/addRelationship"))
                .andExpect(flash().attribute("relationSuccessMessage", "contact1@mail.com est modifié avec succès"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com", roles = "USER")
    void removeConnection_ShouldRedirectWithSuccessMessage() throws Exception {
        doNothing().when(connectionService).removeConnection(anyString(), anyString());

        mockMvc.perform(post("/addRelationship/remove")
                        .param("targetEmail", "contact2@mail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/addRelationship"))
                .andExpect(flash().attribute("relationSuccessMessage", "contact2@mail.com est supprimé avec succès"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com", roles = "USER")
    void showConnectionsPage_ShouldReturnError_WhenUserNotFound() throws Exception {
        when(userService.getUserByEmail(anyString())).thenThrow(new UserNotFoundException("Utilisateur introuvable"));

        mockMvc.perform(get("/addRelationship"))
                .andExpect(redirectedUrl("/login"));
    }
}
