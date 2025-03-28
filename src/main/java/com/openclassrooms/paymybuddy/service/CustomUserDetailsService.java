package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername (String username) throws UsernameNotFoundException {

        return userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("L'utilisateur n'est pas disponible" + username));
    }
}
