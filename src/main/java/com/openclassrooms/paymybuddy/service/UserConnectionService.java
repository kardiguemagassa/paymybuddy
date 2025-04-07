package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.ConnectionAlreadyExistsException;
import com.openclassrooms.paymybuddy.exception.ConnectionNotFoundException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;

import java.util.Set;

public interface UserConnectionService {

    Set<User> getUserConnections(String userEmail);
    Set<User> getPotentialConnections(String userEmail) throws UserNotFoundException;
    void addConnection(String userEmail, String connectionEmail)
            throws UserNotFoundException, ConnectionAlreadyExistsException;
    void updateConnection(String currentUserEmail, String oldConnectionEmail, String newConnectionEmail)
            throws UserNotFoundException;
    void removeConnection(String userEmail, String connectionEmail)
            throws UserNotFoundException, ConnectionNotFoundException;

}
