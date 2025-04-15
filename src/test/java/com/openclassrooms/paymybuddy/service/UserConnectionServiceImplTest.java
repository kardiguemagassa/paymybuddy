package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserConnectionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class UserConnectionServiceImplTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserConnectionServiceImpl userConnectionService;

    private User testUser;
    private User connectionUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@gmail.com");
        testUser.setName("John");
        testUser.setPassword("encodedPassword");

        connectionUser = new User();
        connectionUser.setId(2L);
        connectionUser.setEmail("jane@gmail.com");
        connectionUser.setName("Jane");

        // stocker le Set de user connecter:: pour instant 1 seul user dans le set qui connectionUser
        testUser.setConnections(new HashSet<>(Collections.singletonList(connectionUser)));
    }

    @Test
    void getUserConnections_whenUserExists_shouldReturnUserConnections() {
        log.info("getUserConnections_whenUserExists_shouldReturnUserConnections");

        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findRelationsByEmail(testUser.getEmail())).thenReturn(testUser.getConnections());

        // When
        Set<User> userConnections = userConnectionService.getUserConnections(testUser.getEmail());

        // Then
        assertNotNull(userConnections);
        assertEquals(1, userConnections.size());
        assertTrue(userConnections.contains(connectionUser));
    }

    @Test
    void getUserConnections_whenUserNotFound_shouldThrowException() {
        log.info("getUserConnections_whenUserNotFound_shouldThrowException");
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userConnectionService.getUserConnections("unknown@gmail.com"));
    }

    @Test
    void addConnection_whenValidUsersAndNoExistingConnection_shouldAddConnectionSuccessfully() {
        log.info("addConnection_whenValidUsersAndNoExistingConnection_shouldAddConnectionSuccessfully");

        // Given
        User newConnection = new User();
        newConnection.setId(3L);
        newConnection.setEmail("newEmail@gmail.com");

        // Ouvrir une connection entre deux users
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(newConnection.getEmail())).thenReturn(Optional.of(newConnection));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userConnectionService.addConnection(testUser.getEmail(), newConnection.getEmail());

        // Then
        assertTrue(testUser.getConnections().contains(newConnection));
        verify(userRepository).save(testUser);
    }

    @Test
    void addConnection_whenSelfConnection_shouldThrowException() {
        log.info("addConnection_whenSelfConnection_shouldThrowException");
        assertThrows(IllegalArgumentException.class, () ->
                userConnectionService.addConnection(testUser.getEmail(), testUser.getEmail()));
    }

    @Test
    void addConnection_whenUserNotFound_shouldThrowException() {
        log.info("addConnection_whenUserNotFound_shouldThrowException");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                userConnectionService.addConnection(testUser.getEmail(), "unknown@gmail.com"));
    }

    @Test
    void addConnection_whenConnectionAlreadyExists_shouldThrowException() {
        log.info("addConnection_whenConnectionAlreadyExists_shouldThrowException");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(connectionUser.getEmail())).thenReturn(Optional.of(connectionUser));

        assertThrows(IllegalArgumentException.class, () ->
                userConnectionService.addConnection(testUser.getEmail(), connectionUser.getEmail()));
    }

    @Test
    void addConnection_whenCurrentUserNotFound_shouldThrowRuntimeException() {
        log.info("addConnection_whenCurrentUserNotFound_shouldThrowRuntimeException");
        // Given
        String currentUserEmail = "nonexistent@example.com";
        String targetEmail = "existing@example.com";

        when(userRepository.findByEmail(currentUserEmail)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userConnectionService.addConnection(currentUserEmail, targetEmail));

        assertEquals("Utilisateur introuvable: " + currentUserEmail, exception.getMessage());
        verify(userRepository).findByEmail(currentUserEmail);
        verify(userRepository, never()).findByEmail(targetEmail);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getPotentialConnections_whenUserExists_shouldReturnNonConnectedUsers() throws UserNotFoundException {
        log.info("getPotentialConnections_whenUserExists_shouldReturnNonConnectedUsers");

        // Given
        List<User> potentialConnections = new ArrayList<>();
        User potentialUser = new User();
        potentialUser.setId(3L);
        potentialUser.setEmail("potential@gmail.com");
        potentialConnections.add(potentialUser);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findPotentialConnections(testUser.getEmail(),testUser.getId())).thenReturn(potentialConnections);

        // When
        //récupérer les utilisateurs qui ne sont pas encore connectés à testUser
        Set<User> userConnections = userConnectionService.getPotentialConnections(testUser.getEmail());
        assertNotNull(userConnections);
        assertEquals(1, userConnections.size());
        assertTrue(userConnections.contains(potentialUser));
    }

    @Test
    void getPotentialConnections_whenUserNotFound_shouldThrowException() {
        log.info("getPotentialConnections_whenUserNotFound_shouldThrowException");
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userConnectionService.getPotentialConnections("unknown@gmail.com"));
    }

    @Test
    void updateConnection_whenValidUsersAndExistingConnection_shouldUpdateSuccessfully() throws UserNotFoundException {
        log.info("updateConnection_whenValidUsersAndExistingConnection_shouldUpdateSuccessfully");
        User newConnection = new User();
        newConnection.setId(3L);
        newConnection.setEmail("new@gmail.com");

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(connectionUser.getEmail())).thenReturn(Optional.of(connectionUser));
        when(userRepository.findByEmail(newConnection.getEmail())).thenReturn(Optional.of(newConnection));
        when(userRepository.save(testUser)).thenReturn(testUser);

        userConnectionService.updateConnection(testUser.getEmail(), connectionUser.getEmail(), newConnection.getEmail());

        assertFalse(testUser.getConnections().contains(connectionUser));
        assertTrue(testUser.getConnections().contains(newConnection));
        verify(userRepository).save(testUser);
    }

    @Test
    void updateConnection_whenOldConnectionNotExists_shouldThrowException() {
        log.info("updateConnection_whenOldConnectionNotExists_shouldThrowException");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userConnectionService.updateConnection(
                        testUser.getEmail(), "unknown@gmail.com", "new@gmail.com"));
    }

    @Test
    void updateConnection_whenNewConnectionAlreadyExists_shouldThrowException() {
        log.info("updateConnection_whenNewConnectionAlreadyExists_shouldThrowException");
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(connectionUser.getEmail())).thenReturn(Optional.of(connectionUser));

        assertThrows(IllegalStateException.class, () -> userConnectionService.updateConnection(
                        testUser.getEmail(), connectionUser.getEmail(), connectionUser.getEmail()));
    }

    @Test
    void updateConnection_whenOldConnectionNotInUserConnections_shouldThrowIllegalStateException() {
        log.info("updateConnection_whenOldConnectionNotInUserConnections_shouldThrowIllegalStateException");

        // Given
        User currentUser = new User();
        currentUser.setEmail("current@gmail.com");

        User oldConnection = new User();
        oldConnection.setEmail("old@gmail.com");

        User newConnection = new User();
        newConnection.setEmail("new@gmail.com");

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail(oldConnection.getEmail())).thenReturn(Optional.of(oldConnection));
        when(userRepository.findByEmail(newConnection.getEmail())).thenReturn(Optional.of(newConnection));

        // ancienne connexion n'est pas dans la liste
        currentUser.setConnections(new HashSet<>());

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                userConnectionService.updateConnection(currentUser.getEmail(), oldConnection.getEmail(), newConnection.getEmail())
        );

        assertEquals("L'ancienne connexion n'existe pas", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateConnection_shouldThrowUserNotFoundExceptionWhenCurrentUserNotFound() {
        log.info("updateConnection_shouldThrowUserNotFoundExceptionWhenCurrentUserNotFound");
        // Arrange
        String currentUserEmail = "notfound@example.com";
        String oldConnectionEmail = "old@example.com";
        String newConnectionEmail = "new@example.com";

        when(userRepository.findByEmail(currentUserEmail))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
            userConnectionService.updateConnection(currentUserEmail, oldConnectionEmail, newConnectionEmail));

        verify(userRepository, times(1)).findByEmail(currentUserEmail);
        verify(userRepository, never()).findByEmail(oldConnectionEmail);
        verify(userRepository, never()).findByEmail(newConnectionEmail);
    }

    @Test
    void updateConnection_shouldThrowUserNotFoundExceptionWhenNewConnectionNotFound() {
        log.info("updateConnection_shouldThrowUserNotFoundExceptionWhenNewConnectionNotFound");
        // Arrange
        String currentUserEmail = "current@example.com";
        String oldConnectionEmail = "old@example.com";
        String newConnectionEmail = "notfound@example.com";

        User currentUser = new User();
        User oldConnection = new User();

        when(userRepository.findByEmail(currentUserEmail))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail(oldConnectionEmail))
                .thenReturn(Optional.of(oldConnection));
        when(userRepository.findByEmail(newConnectionEmail))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
            userConnectionService.updateConnection(currentUserEmail, oldConnectionEmail, newConnectionEmail));

        verify(userRepository, times(1)).findByEmail(currentUserEmail);
        verify(userRepository, times(1)).findByEmail(oldConnectionEmail);
        verify(userRepository, times(1)).findByEmail(newConnectionEmail);
    }

    @Test
    void removeConnection_whenConnectionExists_shouldRemoveSuccessfully() {
        log.info("removeConnection_whenConnectionExists_shouldRemoveSuccessfully");

        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(connectionUser.getEmail())).thenReturn(Optional.of(connectionUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userConnectionService.removeConnection(testUser.getEmail(), connectionUser.getEmail());

        // Then
        assertFalse(testUser.getConnections().contains(connectionUser));
        verify(userRepository).save(testUser);
        log.info("Connection successfully removed between {} and {}",
                testUser.getEmail(), connectionUser.getEmail());
    }

    @Test
    void removeConnection_whenTargetUserNotFound_shouldThrowException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userConnectionService.removeConnection("test", "unknown"));
    }

    @Test
    void removeConnection_whenNoConnectionExists_shouldDoNothing() {
        // Given
        User currentUser = new User();
        currentUser.setEmail("user1@test.com");
        currentUser.setConnections(new HashSet<>());

        User targetUser = new User();
        targetUser.setEmail("user2@test.com");

        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("user2@test.com")).thenReturn(Optional.of(targetUser));

        // When
        userConnectionService.removeConnection("user1@test.com", "user2@test.com");

        // Then
        verify(userRepository, never()).save(any());
        assertTrue(currentUser.getConnections().isEmpty());
    }

    @Test
    void removeConnection_shouldThrowRuntimeExceptionWhenTargetUserNotFound() {
        // Arrange
        String currentUserEmail = "current@example.com";
        String targetEmail = "notfound@example.com";

        User currentUser = new User();
        currentUser.setEmail(currentUserEmail);

        when(userRepository.findByEmail(currentUserEmail))
                .thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail(targetEmail))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            userConnectionService.removeConnection(currentUserEmail, targetEmail));

        assertEquals("Utilisateur cible introuvable: " + targetEmail, exception.getMessage());

        // Vérifications
        verify(userRepository, times(1)).findByEmail(currentUserEmail);
        verify(userRepository, times(1)).findByEmail(targetEmail);
        verify(userRepository, never()).save(any());
    }
}
