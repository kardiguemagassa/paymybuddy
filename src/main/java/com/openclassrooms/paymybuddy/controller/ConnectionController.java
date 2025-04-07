package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.serviceImpl.UserConnectionServiceImpl;
import com.openclassrooms.paymybuddy.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/addRelationship")
public class ConnectionController {

    private final UserConnectionServiceImpl connectionService;
    private final UserService userService;

    @GetMapping()
    public String showConnectionsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) throws UserNotFoundException {

        String currentUserEmail = userDetails.getUsername();
        User currentUser = userService.getUserByEmail(currentUserEmail);

        // Récupère les relations et connexions potentielles
        model.addAttribute("relations", connectionService.getUserConnections(currentUserEmail));
        model.addAttribute("potentialConnections", connectionService.getPotentialConnections(currentUserEmail));
        model.addAttribute("user", currentUser);

        return "addRelationship";
    }

    @PostMapping("/add")
    public String addConnection(@RequestParam("email") String targetEmail, @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

        String currentUserEmail = userDetails.getUsername();
        connectionService.addConnection(currentUserEmail, targetEmail);
        redirectAttributes.addFlashAttribute("relationSuccessMessage",targetEmail + " est ajouté avec succès");
        return "redirect:/addRelationship";
    }

    @PostMapping("/update")
    public String updateConnection(@RequestParam String oldConnectionEmail, @RequestParam String newConnectionEmail,
            @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {

        String currentUserEmail = userDetails.getUsername();
        connectionService.updateConnection(currentUserEmail, oldConnectionEmail, newConnectionEmail);
        redirectAttributes.addFlashAttribute("relationSuccessMessage",oldConnectionEmail + " est modifié avec succès");
        return "redirect:/addRelationship";
    }

    @PostMapping("/remove")
    public String removeConnection(@RequestParam String targetEmail, @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {

        String currentUserEmail = userDetails.getUsername();
        connectionService.removeConnection(currentUserEmail, targetEmail);
        redirectAttributes.addFlashAttribute("relationSuccessMessage",targetEmail + " est supprimé avec succès");
        return "redirect:/addRelationship";
    }
}