package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.UserConnectionService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@AllArgsConstructor
@Transactional
public class UserConnectionServiceImpl implements UserConnectionService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;

    @Override
    public Set<User> getUserConnections(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userEmail));

        return userRepository.findRelationsByEmail(userEmail);
    }

    @Transactional
    @Override
    public void addConnection(String currentUserEmail, String targetEmail) {

        if (currentUserEmail.equals(targetEmail)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous ajouter vous-même");
        }

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + currentUserEmail));

        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("L'utilisateur n'existe pas: " + targetEmail));

        // Vérifie si la relation existe déjà
        if (currentUser.getConnections().contains(targetUser)) {
            LOGGER.info("Relation déjà existante entre {} et {}", currentUserEmail, targetEmail);
            throw new IllegalArgumentException("Cette relation exist déjà! veuillez renseigner une autre");
            //return false;
        }

        // Ajout connexion bidirectionnelle
        currentUser.addConnection(targetUser);
        userRepository.save(currentUser);

        LOGGER.info("Nouvelle relation ajoutée: {} <-> {}", currentUserEmail, targetEmail);
    }

    @Override
    public Set<User> getPotentialConnections(String userEmail) throws UserNotFoundException {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        return new HashSet<>(userRepository.findPotentialConnections(userEmail, currentUser.getId()));
    }

    @Override
    public void updateConnection(String currentUserEmail, String oldConnectionEmail, String newConnectionEmail)
            throws UserNotFoundException {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        User oldConnection = userRepository.findByEmail(oldConnectionEmail)
                .orElseThrow(() -> new UserNotFoundException("Ancienne connexion introuvable"));

        User newConnection = userRepository.findByEmail(newConnectionEmail)
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

    }

    @Transactional
    @Override
    public void removeConnection(String currentUserEmail, String targetEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + currentUserEmail));

        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible introuvable: " + targetEmail));

        if (!currentUser.getConnections().contains(targetUser)) {
            LOGGER.info("Aucune relation existante à supprimer entre {} et {}", currentUserEmail, targetEmail);
            return;
        }

        currentUser.removeConnection(targetUser);
        userRepository.save(currentUser);

        LOGGER.info("Relation supprimée: {} -/-> {}", currentUserEmail, targetEmail);
    }

}