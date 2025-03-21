package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.service.ConnectionService;
import com.openclassrooms.paymybuddy.service.userServiceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserServiceImpl userService;

    @GetMapping("/addRelationship")
    public String displayPage() {
        return "addRelationship";
    }

    @PostMapping("/addRelationship/addRelation")
    public String addConnection(@RequestParam String email, User userDetails, Model model) {

        String currentUserEmail = userDetails.getUsername();

        boolean success = connectionService.addUser(currentUserEmail, email);

        User currentUser = userService.findUserByEmail(currentUserEmail).orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<User> relations = connectionService.getUserRelation(currentUser);

        if (success) {
            model.addAttribute("successMessage", "Favorit ajouté avec succès !");
        } else {
            model.addAttribute("errorMessage", "Email existant dans la liste ou utilisateur inexistant en base de données !");
        }

        model.addAttribute("relations", relations);

        return "addRelationship";
    }

}
