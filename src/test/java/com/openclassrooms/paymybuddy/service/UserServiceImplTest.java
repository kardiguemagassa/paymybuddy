package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        LOGGER.debug("Starting test: registerUser_shouldEncodePasswordAndSaveUser");

        // Given
        String plainPassword = "plainPassword";
        String encodedPassword = "encodedTestPassword";
        testUser.setPassword(plainPassword);

        LOGGER.debug("Plain password before encoding: {}", plainPassword);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);

        // When
        userService.registerUser(testUser);

        // Then
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).save(testUser);
        assertEquals(encodedPassword, testUser.getPassword());

        LOGGER.debug("Encoded password after processing: {}", testUser.getPassword());
        LOGGER.debug("Test completed successfully");
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
        LOGGER.debug("Starting test: getUserByEmail_shouldReturnUser");
        when(userRepository.findByEmail("john@email.com")).thenReturn(Optional.of(testUser));
        User user = userService.getUserByEmail("john@email.com");
        assertEquals(testUser.getId(), user.getId());
    }

    @Test
    void getUserByEmail_whenUserNotExists_shouldThrowException() {
        // given
        LOGGER.debug("Starting test: getUserByEmail_shouldThrowException");
        when(userRepository.findByEmail("userNotExist@example.com")).thenReturn(Optional.empty());
        // when then
        assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail("userNotExist@example.com"));
    }

//    @Test
//    void updateUser_shouldUpdateUsernameAndEmail() {
//        // Given
//        User existingUser = new User();
//        existingUser.setId(1L);
//        existingUser.setName("Old Name");
//        existingUser.setEmail("old@email.com");
//
//        User updatedUser = new User();
//        updatedUser.setName("New Name");
//        updatedUser.setEmail("new@email.com");
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
//        when(userRepository.save(any(User.class))).thenReturn(existingUser);
//
//        // Configurez le mock pour securityValidationImpl
//        when(securityValidationImpl.validateEmail("new@email.com")).thenReturn(true);
//        when(securityValidationImpl.validateEmailFormat("new@email.com")).thenReturn(true);
//
//        // When
//        User result = userService.updateUser(1L, updatedUser);
//
//        // Then
//        assertThat(result.getName()).isEqualTo("New Name");
//        assertThat(result.getEmail()).isEqualTo("new@email.com");
//    }

//    @Test
//    void updateUser_shouldUpdateUsernameAndEmail() throws Exception {
//        // given
//        LOGGER.debug("Starting test: updateUser_shouldUpdateUsernameAndEmail");
//
//        // Configuration du répertoire de test
//        Path uploadPath = Paths.get("target/test-uploads");
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        // Injection directe sans Spring
//        Field uploadDirField = UserServiceImpl.class.getDeclaredField("uploadDir");
//        uploadDirField.setAccessible(true);
//        uploadDirField.set(userService, uploadPath.toString());
//
//        User existingUser = new User();
//        existingUser.setEmail("john@email.com");
//        existingUser.setName("John");
//        existingUser.setPassword("encodedPassword");
//
//        when(userRepository.findByEmail("john@email.com")).thenReturn(Optional.of(existingUser));
//        when(userRepository.findByEmail("jane@gmail.com")).thenReturn(Optional.empty());
//        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        MultipartFile mockFile = mock(MultipartFile.class);
//        when(mockFile.isEmpty()).thenReturn(false);
//        when(mockFile.getOriginalFilename()).thenReturn("profile.jpg");
//        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
//
//        // when
//        User updatedUser = userService.updateUser("john@email.com", "newUsername", "jane@gmail.com", mockFile);
//
//        // then
//        assertNotNull(updatedUser);
//        assertEquals("newUsername", updatedUser.getName());
//        assertEquals("jane@gmail.com", updatedUser.getEmail());
//        assertNotNull(updatedUser.getProfileImageUrl());
//
//        // Nettoyage
//        Files.walk(uploadPath)
//                .sorted(Comparator.reverseOrder())
//                .map(Path::toFile)
//                .forEach(File::delete);
//    }
}