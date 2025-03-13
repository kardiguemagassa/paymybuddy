package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.util.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername (String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("L'utilisateur n'est pas disponible" + username));

        return new CustomUserDetails(user);
    }
}
