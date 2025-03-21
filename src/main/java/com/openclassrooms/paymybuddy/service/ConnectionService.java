package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Connection;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.enttity.UserConnectionId;
import com.openclassrooms.paymybuddy.repository.ConnectionRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ConnectionService {

    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;

    @Transactional
    public boolean addUser(String userEmail, String relationEmail) {

        Optional<User> userOptional = userRepository.findUserByEmail(userEmail);
        Optional<User> friendOptional = userRepository.findUserByEmail(relationEmail);

        if (userOptional.isEmpty() || friendOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        User relation = friendOptional.get();

        // Check if the connection already exists
        UserConnectionId connectionId = new UserConnectionId(user.getId(), relation.getId());
        if (connectionRepository.existsById(connectionId)) {
            return false;
        }

        // Creat new connection
        Connection connection = new Connection(connectionId, user, relation);
        connectionRepository.save(connection);

        return true;
    }

    public List<User> getUserRelation(User user) {

        List<Connection> connections = connectionRepository.findConnectionsByUser(user.getId());

        return connections.stream()
                .map(connection -> connection.getUser().equals(user) ?
                        connection.getConnection() : connection.getUser()).toList();
    }
}
