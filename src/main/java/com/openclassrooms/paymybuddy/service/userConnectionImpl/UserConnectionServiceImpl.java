package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
public class ConnectionService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;

    @Transactional
    public boolean addConnection(String currentUserEmail, String targetEmail) {
        User currentUser = userRepository.findUserByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + currentUserEmail));

        User targetUser = userRepository.findUserByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible introuvable: " + targetEmail));

        // Vérifie si la relation existe déjà
        if (currentUser.getConnections().contains(targetUser)) {
            LOGGER.info("Relation déjà existante entre {} et {}", currentUserEmail, targetEmail);
            return false;
        }

        // Ajoute la connexion bidirectionnelle
        currentUser.addConnection(targetUser);
        userRepository.save(currentUser);

        LOGGER.info("Nouvelle relation ajoutée: {} <-> {}", currentUserEmail, targetEmail);
        return true;
    }

    public Set<User> getUserConnections(String userEmail) {
        User user = userRepository.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userEmail));

        // Utilise la méthode du repository optimisée
        return userRepository.findRelationsByEmail(userEmail);
    }

    public boolean updateConnection(String currentUserEmail, String oldConnectionEmail, String newConnectionEmail) throws UserNotFoundException {
        User currentUser = userRepository.findUserByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        User oldConnection = userRepository.findUserByEmail(oldConnectionEmail)
                .orElseThrow(() -> new UserNotFoundException("Ancienne connexion introuvable"));

        User newConnection = userRepository.findUserByEmail(newConnectionEmail)
                .orElseThrow(() -> new UserNotFoundException("Nouvelle connexion introuvable"));

        if (!currentUser.getConnections().contains(oldConnection)) {
            throw new IllegalStateException("L'ancienne connexion n'existe pas");
        }

        if (currentUser.getConnections().contains(newConnection)) {
            throw new IllegalStateException("La nouvelle connexion existe déjà");
        }

        currentUser.removeConnection(oldConnection);
        currentUser.addConnection(newConnection);
        userRepository.save(currentUser);

        return true;
    }

    @Transactional
    public boolean removeConnection(String currentUserEmail, String targetEmail) {
        User currentUser = userRepository.findUserByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + currentUserEmail));

        User targetUser = userRepository.findUserByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible introuvable: " + targetEmail));

        if (!currentUser.getConnections().contains(targetUser)) {
            LOGGER.info("Aucune relation existante à supprimer entre {} et {}", currentUserEmail, targetEmail);
            return false;
        }

        currentUser.removeConnection(targetUser);
        userRepository.save(currentUser);

        LOGGER.info("Relation supprimée: {} -/-> {}", currentUserEmail, targetEmail);
        return true;
    }
}