package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
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
    private final SecurityValidationImpl securityValidationImpl;
    @Value("${file.upload-dir:src/main/resources/static/uploads/}")
    private String uploadDir;

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
        securityValidationImpl.validateEmail(newEmail);

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
        //String uploadDir = "src/main/resources/static/uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @Override
    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword, String confirmPassword)
            throws UserNotFoundException, InvalidPasswordException, PasswordMismatchException {

        try {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        String message = "Utilisateur non trouvé avec l'email: " + email;
                        LOGGER.error(message);
                        return new UserNotFoundException(message);
                    });

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                String message = "Mot de passe actuel incorrect: " + email;
                LOGGER.warn(message);
                throw new InvalidPasswordException(message);
            }

            if (!StringUtils.equals(newPassword, confirmPassword)) {
                String message = "Les nouveaux mots de passe ne correspondent pas: " + email;
                LOGGER.warn(message);
                throw new PasswordMismatchException(message);
            }

            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                String message = "Le nouveau mot de passe doit être différent de l'actuel: " + email;
                LOGGER.warn(message);
                throw new InvalidPasswordException(message);
            }

            // Validation des règles du nouveau mot de passe
            securityValidationImpl.validatePassword(newPassword);

            // Mise à jour du mot de passe
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            LOGGER.info("Mot de passe mis à jour avec succès pour l'utilisateur: {}", email);

        } catch (Exception e) {
            LOGGER.error("Échec de la mise à jour du mot de passe pour l'email: " + email, e);
            throw e;
        }
    }

}