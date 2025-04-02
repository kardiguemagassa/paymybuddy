package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.userConnectionImpl.UserConnectionServiceImpl;
import com.openclassrooms.paymybuddy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/addRelationship")
public class ConnectionController {

    private final UserConnectionServiceImpl connectionService;
    private final UserService userService;

    @GetMapping()
    public String showConnectionsPage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws UserNotFoundException {

        String currentUserEmail = userDetails.getUsername();
        User currentUser = userService.getUserByEmail(currentUserEmail);

        // Récupère les relations et connexions potentielles
        Set<User> relations = connectionService.getUserConnections(currentUserEmail);
        Set<User> potentialConnections = connectionService.getPotentialConnections(currentUserEmail);

        model.addAttribute("relations", relations);
        model.addAttribute("potentialConnections", potentialConnections);
        model.addAttribute("user", currentUser);

        return "addRelationship";
    }

    @PostMapping("/add")
    public String addConnection(
            @RequestParam("email") String targetEmail,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String currentUserEmail = userDetails.getUsername();

        try {
            boolean added = connectionService.addConnection(currentUserEmail, targetEmail);

            if (added) {
                redirectAttributes.addFlashAttribute("successMessage", "Connexion ajoutée avec succès !");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "La relation existe déjà ou l'email est incorrect.");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/addRelationship";
    }

    @PostMapping("/update")
    public String updateConnection(
            @RequestParam String oldConnectionEmail,
            @RequestParam String newConnectionEmail,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

            String currentUserEmail = userDetails.getUsername();

        try {
            boolean updated = connectionService.updateConnection(currentUserEmail, oldConnectionEmail, newConnectionEmail);

            if (updated) {
                redirectAttributes.addFlashAttribute("success", "Connexion modifiée avec succès");
            } else {
                redirectAttributes.addFlashAttribute("warning", "Échec de la modification");
            }
        } catch (RuntimeException | UserNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/addRelationship";
    }

    @PostMapping("/remove")
    public String removeConnection(
            @RequestParam String targetEmail,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String currentUserEmail = userDetails.getUsername();

        try {
            boolean removed = connectionService.removeConnection(currentUserEmail, targetEmail);

            if (removed) {
                redirectAttributes.addFlashAttribute("success", "Connexion supprimée avec succès");
            } else {
                redirectAttributes.addFlashAttribute("warning", "Aucune connexion existante à supprimer");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/addRelationship";
    }
}