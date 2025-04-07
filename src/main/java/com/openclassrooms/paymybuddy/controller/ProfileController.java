package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import com.openclassrooms.paymybuddy.service.UserService;
import com.openclassrooms.paymybuddy.service.serviceImpl.SecurityValidationImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@AllArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final SecurityValidationImpl securityValidationImpl;

    @GetMapping("/profile")
    public String showProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) throws UserNotFoundException {
        String email = userDetails.getUsername();
        User currentUser = userService.getUserByEmail(email);
        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String newUsername,
            @RequestParam String newEmail,
            @RequestParam("profileImage") MultipartFile profileImage,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {

        User updatedUser = userService.updateUser(userDetails.getUsername(), newUsername, newEmail, profileImage);
        securityValidationImpl.updateSecurityContext(updatedUser, request);
        redirectAttributes.addFlashAttribute("successProfile", "Profil mis à jour avec succès");
        return "redirect:/login";
    }

    @PostMapping("/profile/update-password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) throws UserNotFoundException, PasswordMismatchException, InvalidPasswordException {

        userService.updatePassword(userDetails.getUsername(), currentPassword, newPassword, confirmPassword);
        redirectAttributes.addFlashAttribute("successPassword", "Mot de passe mis à jour avec succès");
        return "redirect:/login";
    }
}