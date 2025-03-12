package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.repository.ConnectionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final PasswordEncoder passwordEncoder;

//    public User registerUser (String username, String email, String password) {
//
//        if (userRepository.findByEmail(email).isPresent()) {
//            throw new RuntimeException("Email already in use");
//        }
//
//        //String encodedPassword = passwordEncoder.encode(password);
//        //User user = new User(username, email, encodedPassword, new ArrayList<>());
//        User user = new User(username, email, password, new ArrayList<>());
//
//        return userRepository.save(user);
//    }

    public User register (User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByEmail (String email) {
        return userRepository.findByEmail(email);
    }


    public User loginUser(String email, String encryptedPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

//        if (!passwordEncoder.matches(encryptedPassword, user.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }

        return user;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    public void addFriend(String userEmail, String friendEmail) {
        User user = findUserByEmail(userEmail);
        User friend = findUserByEmail(friendEmail);

        if (!user.getConnections().contains(friend)) {
            user.getConnections().add(friend);
            userRepository.save(user);
        }
    }

    public Object getCurrentUserProfile() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String username = authentication.getName();
            return userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }
        return null;
    }
}
