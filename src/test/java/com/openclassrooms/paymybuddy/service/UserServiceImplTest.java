package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.EmailExistsException;
import com.openclassrooms.paymybuddy.exception.InvalidPasswordException;
import com.openclassrooms.paymybuddy.exception.PasswordMismatchException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImplTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityValidationImpl securityValidationImpl;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        System.setProperty("mockito.inline.mockmaker", "false");
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@gmail.com");
        testUser.setName("John");
        testUser.setPassword("encodedPassword");

        lenient().when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(testUser));
        lenient().when(passwordEncoder.encode("JohnDoe")).thenReturn("encodedPassword");
        lenient().when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
    }

    @Test
    void registerUser_shouldEncodePasswordAndSaveUser() {
        LOGGER.info("Starting test: registerUser_shouldEncodePasswordAndSaveUser");

        // Given
        String plainPassword = "plainPassword";
        String encodedPassword = "encodedTestPassword";
        testUser.setPassword(plainPassword);

        LOGGER.info("Plain password before encoding: {}", plainPassword);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);

        // When
        userService.registerUser(testUser);

        // Then
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).save(testUser);
        assertEquals(encodedPassword, testUser.getPassword());

        LOGGER.info("Encoded password after processing: {}", testUser.getPassword());
        LOGGER.info("Test completed successfully");
    }

    @Test
    void showRealPasswordEncoding() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        String rawPassword = "plainPassword";
        String realEncoded = realEncoder.encode(rawPassword);

        LOGGER.info("REAL ENCODING EXAMPLE:");
        LOGGER.info("Raw password: {}", rawPassword);
        LOGGER.info("Encoded password: {}", realEncoded);

        assertTrue(realEncoded.startsWith("$2a$")); // Vérification du format BCrypt
        assertTrue(realEncoder.matches(rawPassword, realEncoded));
    }

    @Test
    void getUserByEmail_whenUserExists_shouldReturnUser() {
        // given
        LOGGER.info("Starting test: getUserByEmail_shouldReturnUser");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        User user = userService.getUserByEmail(testUser.getEmail());
        assertEquals(testUser.getId(), user.getId());
    }

    @Test
    void getUserByEmail_whenUserNotExists_shouldThrowException() {
        // given
        LOGGER.info("Starting test: getUserByEmail_shouldThrowException");
        when(userRepository.findByEmail("userNotExist@gmail.com")).thenReturn(Optional.empty());
        // when then
        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("userNotExist@gmail.com"));
    }

    @Test
    void findUserOptionalByEmail_whenUserExists_shouldReturnUser() {
        LOGGER.info("Starting test: findUserOptionalByEmail_shouldReturnUser");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        Optional<User> user = userService.findUserByEmail(testUser.getEmail());
        assertEquals(testUser.getId(), user.orElse(null).getId());
    }

    @Test
    void updateUser_shouldUpdateUsernameAndEmail() throws Exception {
        // given
        LOGGER.info("Starting test: updateUser_shouldUpdateUsernameAndEmail");

        // Configuration du répertoire de test
        Path uploadPath = Paths.get("target/test-uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Injection directe sans Spring
        Field uploadDirField = UserServiceImpl.class.getDeclaredField("uploadDir");
        uploadDirField.setAccessible(true);
        uploadDirField.set(userService, uploadPath.toString());

        User existingUser = new User();
        existingUser.setEmail("john@email.com");
        existingUser.setName("John");
        existingUser.setPassword("encodedPassword");

        when(userRepository.findByEmail("john@email.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("jane@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("profile.jpg");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        // when
        User updatedUser = userService.updateUser("john@email.com", "newUsername", "jane@gmail.com", mockFile);

        // then
        assertNotNull(updatedUser);
        assertEquals("newUsername", updatedUser.getName());
        assertEquals("jane@gmail.com", updatedUser.getEmail());
        assertNotNull(updatedUser.getProfileImageUrl());

        // Nettoyage
        Files.walk(uploadPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void  updateUser_whenNewEmailAlreadyExists_shouldThrowEmailExistsException() {
        LOGGER.info("Starting test: updateUser_whenNewEmailAlreadyExists_shouldThrowEmailExistsException");

        // Given
        String currentEmail = "john@email.com";
        String newEmail = "john@gmail.com";
        String newUsername = "newUsername";

        User existingUser = new User();
        existingUser.setEmail(currentEmail);
        User anotherUser = new User();
        anotherUser.setEmail(newEmail);

        when(userRepository.findByEmail(currentEmail)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.of(anotherUser));

        // When & Then
        assertThrows(EmailExistsException.class, () -> userService.updateUser(currentEmail,newUsername,newEmail,null));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenImageStorageFails_shouldThrowIOException() throws Exception {
        LOGGER.info("Starting test: updateUser_whenImageStorageFails_shouldThrowIOException");

        // Configuration du répertoire de test
        Path uploadPath = Paths.get("target/test-uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Injection sans Spring
        Field uploadDirField = UserServiceImpl.class.getDeclaredField("uploadDir");
        uploadDirField.setAccessible(true);
        uploadDirField.set(userService, uploadPath.toString());

        // Given
        String currentEmail = "john@gmail.com";
        String newEmail = "new@gmail.com";
        String newUsername = "newUsername";

        User existingUser = new User();
        existingUser.setEmail(currentEmail);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("profile.jpg");
        when(mockFile.getInputStream()).thenThrow(new IOException("Simulated storage error"));

        when(userRepository.findByEmail(currentEmail)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(newEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IOException.class, () ->
                userService.updateUser(currentEmail, newUsername, newEmail, mockFile));

        //  user n'a pas été modifié
        verify(userRepository, never()).save(any());

        // Nettoyage
        Files.walk(uploadPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

        @Test
    void updatePassword_whenCurrentPasswordIsCorrect_shouldUpdatePassword() throws PasswordMismatchException, InvalidPasswordException {
        LOGGER.info("Starting test: updatePassword_whenCurrentPasswordIsCorrect_shouldUpdatePassword");

        // Given
        String currentPassword = "correctPassword";
        String newPassword = "newSecurePassword123";
        String confirmPassword = "newSecurePassword123";

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(newPassword, testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");

        // When
        userService.updatePassword(testUser.getEmail(), currentPassword, newPassword, confirmPassword);

        // Then
        verify(userRepository).save(testUser);
        assertEquals("newEncodedPassword", testUser.getPassword());
    }

    @Test
    void updatePassword_whenUserNotFound_shouldThrowException() {
        LOGGER.info("Starting test: updatePassword_whenUserNotFound_shouldThrowException");

        // Given
        String email = "userNotExist@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () ->
                userService.updatePassword(email, "any", "newPass", "newPass"));
    }

    @Test
    void updatePassword_whenCurrentPasswordIncorrect_shouldThrowException() {
        LOGGER.info("Starting test: updatePassword_whenCurrentPasswordIncorrect_shouldThrowException");

        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // When & Then
        assertThrows(InvalidPasswordException.class, () ->
                userService.updatePassword(testUser.getEmail(), "wrongPassword", "newPass", "newPass"));
    }

    @Test
    void updatePassword_whenPasswordsDontMatch_shouldThrowException() {
        LOGGER.info("Starting test: updatePassword_whenPasswordsDontMatch_shouldThrowException");

        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", testUser.getPassword())).thenReturn(true);

        // When & Then
        assertThrows(PasswordMismatchException.class, () ->
                userService.updatePassword(testUser.getEmail(), "correctPassword", "newPass", "differentPass"));
    }

    @Test
    void updatePassword_whenNewPasswordSameAsCurrent_shouldThrowException() {
        LOGGER.info("Starting test: updatePassword_whenNewPasswordSameAsCurrent_shouldThrowException");

        // Given
        String currentPassword = "correctPassword";
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);

        // When & Then
        assertThrows(InvalidPasswordException.class, () ->
                userService.updatePassword(testUser.getEmail(), currentPassword, currentPassword, currentPassword));
    }

    @Test
    void updatePassword_whenNewPasswordInvalid_shouldThrowException() throws InvalidPasswordException {
        LOGGER.info("Starting test: updatePassword_whenNewPasswordInvalid_shouldThrowException");

        // Given
        String currentPassword = "correctPassword";
        String invalidPassword = "weakPassword";

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        doThrow(new InvalidPasswordException("Password too weakPassword"))
                .when(securityValidationImpl).validatePassword(invalidPassword);

        // When & Then
        assertThrows(InvalidPasswordException.class, () ->
                userService.updatePassword(testUser.getEmail(), currentPassword, invalidPassword, invalidPassword));
    }
}