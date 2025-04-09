package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Historic;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.repository.HistoricRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.serviceImpl.HistoricServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class HistoricServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private HistoricRepository historicRepository;
    @Mock
    private Pageable pageable;

    @InjectMocks
    private HistoricServiceImpl historicService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("john@gmail.com");

        Historic testHistoric = new Historic();
        testHistoric.setId(1L);
    }

    @Test
    void getUserByEmail_whenUserExists_shouldReturnUser() {
        log.info("testGetUserByEmail_whenUserExists");
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When
        User result = historicService.getUserByEmail(testUser.getEmail());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void getUserByEmail_whenUserNotExists_shouldThrowException() {
        log.info("testGetUserByEmail_whenUserNotExists");
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> historicService.getUserByEmail("unknown@example.com"));

        verify(userRepository).findByEmail("unknown@example.com");
    }

    @Test
    void getUserHistoricPaginated_whenNoHistoric_shouldReturnEmptyPage() {
        log.info("testGetUserHistoricPaginated_whenNoHistoric");
        // Given
        Page<Historic> emptyPage = new PageImpl<>(Collections.emptyList());
        when(historicRepository.findUserHistoryPaginated(anyLong(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<Historic> result = historicService.getUserHistoricPaginated(1L, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(historicRepository).findUserHistoryPaginated(1L, pageable);
    }
}
