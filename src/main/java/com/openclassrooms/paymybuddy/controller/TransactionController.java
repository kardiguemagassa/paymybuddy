package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/transaction")
    public String displayPge() {
        return "transaction";
    }

    @PostMapping("/send")
    public ResponseEntity sendTransaction(
            @RequestParam String senderEmail,
            @RequestParam String receiverEmail,
            @RequestParam double amount,
            @RequestParam String description,
            @RequestParam String currency
    ) {
        Transaction transaction = transactionService.sendMoney(senderEmail, receiverEmail, amount, description, currency);
        return ResponseEntity.ok(transaction);

    }
}
