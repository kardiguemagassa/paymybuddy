package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.*;
import com.openclassrooms.paymybuddy.service.CustomUserDetailsService;
import com.openclassrooms.paymybuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
    private CustomUserDetailsService customUserDetailsService;
    private final Logger LOGGER = LogManager.getLogger(ProfileController.class);

    @GetMapping("/profile")
    public String showProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        try {
            String email = userDetails.getUsername();
            User currentUser = userService.getUserByEmail(email);
            model.addAttribute("user", currentUser);
        } catch (UserNotFoundException e) {
            model.addAttribute("error", "Profil non trouvé. Veuillez vous reconnecter.");
            redirectAttributes.addFlashAttribute("error", "Profil non trouvé. Veuillez vous reconnecter.");
            return "redirect:/login";
        }
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String newUsername,
            @RequestParam String newEmail,
            @RequestParam("profileImage") MultipartFile profileImage,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        try {
            User updatedUser = userService.updateUser(
                    userDetails.getUsername(),
                    newUsername,
                    newEmail,
                    profileImage
            );

            updateSecurityContext(updatedUser, request);
            redirectAttributes.addFlashAttribute("successProfile", "Profil mis à jour avec succès");
        } catch (EmailExistsException e) {
            redirectAttributes.addFlashAttribute("errorProfile", "Cet email est déjà utilisé");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorProfile", "Erreur image: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorProfile", "Erreur lors de la mise à jour");
            LOGGER.error("Erreur updateProfile", e);
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/update-password")
    public String updatePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updatePassword(
                    userDetails.getUsername(),
                    currentPassword,
                    newPassword,
                    confirmPassword
            );
            redirectAttributes.addFlashAttribute("successPassword", "Mot de passe mis à jour avec succès");
        } catch (UserNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorPassword", "Session expirée");
        } catch (InvalidPasswordException e) {
            redirectAttributes.addFlashAttribute("errorPassword", e.getMessage());
        } catch (PasswordMismatchException e) {
            redirectAttributes.addFlashAttribute("errorPassword", e.getMessage());
        }

        return "redirect:/profile#password";
        //return "redirect:/login";
    }

    private void updateSecurityContext(User user, HttpServletRequest request) {
        UserDetails newUserDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                newUserDetails,
                newUserDetails.getPassword(),
                newUserDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // Rafraîchir la session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
    }
}