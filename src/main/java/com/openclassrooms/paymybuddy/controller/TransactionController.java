package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.service.TransactionService;
import com.openclassrooms.paymybuddy.service.userServiceImpl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserServiceImpl userService;

    @GetMapping("/transaction")
    public String showTransactionPage(Model model) {

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        List<User> users = userService.getUsers();
        model.addAttribute("users", users);

        List<Transaction> transactions = transactionService.getAllTransactionsForUser(userEmail);
        model.addAttribute("transactions", transactions);

        return "transaction";
    }

    @PostMapping("/transaction/send")
    public String makeTransaction(
            @RequestParam String senderEmail,
            @RequestParam String receiverEmail,
            @RequestParam double amount,
            @RequestParam String description
            //@RequestParam String currency
    ) {
        User sender = userService.findUserByEmail(senderEmail).orElseThrow();
        User receiver = userService.findUserByEmail(receiverEmail).orElseThrow();

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        //transaction.setCurrency(currency);
        transaction.setExecutionDate(LocalDateTime.now());

        transactionService.makeTransaction(transaction);
        return "redirect:/transaction";
    }

}
