package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.transactionImpl.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Random;

@Controller
@RequiredArgsConstructor
public class BalanceController {

    private final TransactionServiceImpl transactionService;
    private final Random random = new Random();

    @GetMapping("/addBalance")
    public String showAddBalancePage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws UserNotFoundException {

        String email = userDetails.getUsername();
        User currentUser = transactionService.getUserByTransactionEmail(email);

        model.addAttribute("user", currentUser);
        model.addAttribute("currentBalance", currentUser.getBalance());
        return "balance";
    }

    @PostMapping("/addBalance")
    public String addBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String randomAmount,
            RedirectAttributes redirectAttributes) {

        try {
            String email = userDetails.getUsername();
            double amountToAdd;

            if (randomAmount != null && randomAmount.equals("random")) {

                amountToAdd = 10 + (2000 - 10) * random.nextDouble();
                amountToAdd = Math.round(amountToAdd * 100.0) / 100.0; // Arrondi à 2 décimales
            } else if (amount != null && amount > 0) {
                amountToAdd = amount;
            } else {
                throw new IllegalArgumentException("Montant invalide");
            }

            User user = transactionService.addBalance(email, amountToAdd);

            String formattedAmount = String.format("%.2f", amountToAdd);
            String formatteBalance = String.format("%.2f", user.getBalance());

            redirectAttributes.addFlashAttribute("success",
                    "Rechargement réussi ! Montant ajouté: " + formattedAmount + " €. Nouveau solde: " + formatteBalance + " €");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erreur lors du rechargement: " + e.getMessage());
        }

        return "redirect:/addBalance";
    }
}
