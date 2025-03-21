package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.EmailExistException;
import com.openclassrooms.paymybuddy.exception.NotAnImageFileException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.exception.UsernameExistException;
import com.openclassrooms.paymybuddy.service.userServiceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RequiredArgsConstructor
@Controller
public class ProfileController {

    private final UserServiceImpl userService;

    @GetMapping("/profile")
    public String profile(Model model) {
        User currentUserProfile = userService.getCurrentUserProfile();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            model.addAttribute("username", currentUsername);
        }

        model.addAttribute("user", currentUserProfile);
        System.out.println("PROFILE");
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateUserProfile(
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newEmail") String newEmail,
            @RequestParam("profileImage") MultipartFile profileImage,
            Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();

            try {
                User updatedUser = userService.updateUser(currentUsername, newUsername, newEmail, profileImage);
                model.addAttribute("user", updatedUser);
                model.addAttribute("successMessage", "Profil mis à jour avec succès !");
            } catch (UserNotFoundException | UsernameExistException | EmailExistException | IOException | NotAnImageFileException e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
        }

        return "redirect:/profile";
    }
}
