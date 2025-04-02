package com.openclassrooms.paymybuddy.service.userServiceImpl;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    @Override
    public User getUserByEmail(String email) throws UserNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé avec l'email: " + email));
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional()
    public User updateUser(String email, String newUsername, String newEmail, MultipartFile profileImage)
            throws IOException, UserNotFoundException {

        User user = getUserByEmail(email);

        if (!email.equals(newEmail)) {
            userRepository.findByEmail(newEmail).ifPresent(u -> {
                throw new EmailExistsException("Cet email est déjà utilisé");
            });
        }

        try {
            if (profileImage != null && !profileImage.isEmpty()) {
                String fileName = storeProfileImage(profileImage);
                user.setProfileImageUrl("/uploads/" + fileName);
            }

            user.setName(newUsername);
            user.setEmail(newEmail);
            return userRepository.save(user);

        } catch (IOException e) {
            LOGGER.error("Échec de la mise à jour de l'utilisateur", e);
            throw e;
        }
    }

    private String storeProfileImage(MultipartFile file) throws IOException {
        String uploadDir = "src/main/resources/static/uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @Override
    public void updatePassword(String email, String currentPassword, String newPassword, String confirmPassword)
            throws UserNotFoundException, InvalidPasswordException, PasswordMismatchException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Le mot de passe actuel est incorrect");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("Les nouveaux mots de passe ne correspondent pas");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new InvalidPasswordException("Le nouveau mot de passe doit être différent de l'actuel");
        }

        validatePassword(newPassword);

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        LOGGER.info("Mot de passe mis à jour pour l'utilisateur: {}", email);
    }

    private void validatePassword(String password) throws InvalidPasswordException {

        if (password.length() < 8) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins 8 caractères");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins un caractère spécial");
        }

        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins un chiffre");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins une majuscule");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins une minuscule");
        }
    }
}