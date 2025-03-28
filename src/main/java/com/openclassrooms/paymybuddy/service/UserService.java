package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> getUsers();

    void registerUser(User user) throws UserNotFoundException, UsernameExistException;

    User findUserByUsername(String username);

    Optional<User> findUserByEmail(String email);

    User updateUser(String currentUsername, String newUsername, String newEmail, MultipartFile profileImage)
            throws UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException, EmailExistException;

    void deleteUser(String username) throws IOException;

    User updateProfileImage(String username, MultipartFile profileImage)
            throws UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException, EmailExistException;

}
