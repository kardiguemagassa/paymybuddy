package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Optional;


public interface UserService {

    void registerUser(User user) throws UserNotFoundException, UsernameExistException;
    Optional<User> findUserByEmail(String email);
    User getUserByEmail(String email) throws UserNotFoundException;
    User updateUser(String currentUsername, String newUsername, String newEmail, MultipartFile profileImage)
            throws UserNotFoundException, UsernameExistException, IOException, NotAnImageFileException, EmailExistException;
    void updatePassword(String email, String currentPassword, String newPassword, String confirmPassword)
            throws UserNotFoundException, InvalidPasswordException, PasswordMismatchException;
}