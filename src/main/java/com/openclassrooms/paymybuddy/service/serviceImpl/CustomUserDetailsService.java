package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.enttity.User;
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
    public UserDetails loadUserByUsername (String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("L'utilisateur n'est pas disponible" + email));

       return new org.springframework.security.core.userdetails.User(user.getEmail(),user.getPassword(),user.getAuthorities());
    }
}
