package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@RequestMapping("/addBalance")
public class BalanceController {

    private final TransactionServiceImpl transactionService;

    @GetMapping
    public String showAddBalancePage(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws UserNotFoundException {

        String email = userDetails.getUsername();
        User currentUser = transactionService.getUserByTransactionEmail(email);

        model.addAttribute("user", currentUser);
        model.addAttribute("currentBalance", currentUser.getBalance());
        return "balance";
    }

    @PostMapping
    public String addBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String randomAmount,
            RedirectAttributes redirectAttributes) {

            String email = userDetails.getUsername();
            User updatedUser = transactionService.addBalance(email, amount, randomAmount);
            String successMessage = transactionService.getFormattedBalanceUpdateMessage(updatedUser, amount);

            redirectAttributes.addFlashAttribute("success", successMessage);

        return "redirect:/addBalance?success" + URLEncoder.encode(successMessage,StandardCharsets.UTF_8);
    }
}
