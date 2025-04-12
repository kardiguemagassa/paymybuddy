package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.config.WebSecurityConfig;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.*;
import com.openclassrooms.paymybuddy.service.UserService;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import com.openclassrooms.paymybuddy.controller.controllerTestConfig.MockSecurityBeansConfig;
import com.openclassrooms.paymybuddy.controller.controllerTestConfig.ProfileControllerTestConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(controllers =  ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebSecurityConfig.class, MockSecurityBeansConfig.class,ProfileControllerTestConfig.class})
public class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityValidationImpl securityValidationImpl;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("john@gmail.com");
        testUser.setEmail("testuser");

        // Réinitialisation des mocks
        Mockito.reset(userService, securityValidationImpl);
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void showProfilePage_ShouldReturnProfileView() throws Exception {
        when(userService.getUserByEmail("john@gmail.com")).thenReturn(testUser);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", testUser));

        verify(userService).getUserByEmail("john@gmail.com");
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void showProfilePage_WhenUserNotFound_ShouldRedirectToLogin() throws Exception {
        when(userService.getUserByEmail("john@gmail.com")).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(result -> assertInstanceOf(UserNotFoundException.class, result.getResolvedException()));
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void updateProfile_ShouldRedirectToLoginWithSuccessFlash() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profileImage",
                "test.jpg",
                "image/jpeg",
                "test image".getBytes());

        when(userService.updateUser(anyString(), anyString(), anyString(), any()))
                .thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update")
                        .file(file)
                        .param("newUsername", "newuser")
                        .param("newEmail", "new@example.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successProfile"));

        verify(userService).updateUser(eq("john@gmail.com"), eq("newuser"), eq("new@example.com"), any());
        verify(securityValidationImpl).updateSecurityContext(eq(testUser), any());
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void updateProfile_WhenEmailExists_ShouldRedirectWithFlashAttribute() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "profileImage",
                "test.jpg",
                "image/jpeg",
                "test image".getBytes());

        when(userService.updateUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(new EmailExistException("Email already exists"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/profile/update")
                        .file(file)
                        .param("newUsername", "newuser")
                        .param("newEmail", "existing@example.com")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("errorProfile"))
                .andExpect(flash().attribute("errorProfile", "Email already exists"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void updatePassword_ShouldRedirectToLoginWithSuccessFlash() throws Exception {
        doNothing().when(userService).updatePassword(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/profile/update-password")
                        .param("currentPassword", "oldPass")
                        .param("newPassword", "newPass")
                        .param("confirmPassword", "newPass")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successPassword"));

        verify(userService).updatePassword(eq("john@gmail.com"), eq("oldPass"), eq("newPass"), eq("newPass"));
    }

    @Test
    @WithMockUser(username = "john@gmail.com")
    void updatePassword_WhenPasswordsDontMatch_ShouldRedirectWithFlashAttribute() throws Exception {
        doThrow(new PasswordMismatchException("Passwords don't match"))
                .when(userService).updatePassword(anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/profile/update-password")
                        .param("currentPassword", "oldPass")
                        .param("newPassword", "newPass")
                        .param("confirmPassword", "differentPass")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile#password"))
                .andExpect(flash().attributeExists("errorPassword"))
                .andExpect(flash().attribute("errorPassword", "Passwords don't match"));
    }

}
