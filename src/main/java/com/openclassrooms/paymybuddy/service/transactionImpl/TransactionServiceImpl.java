package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private static final double FEE_PERCENTAGE = 0.005; // 0.5%

    @Transactional
    public Transaction makeTransaction(String senderEmail, String receiverEmail,
                                       double amount, String description) throws UserNotFoundException, InsufficientBalanceException {

        User sender = userService.getUserByEmail(senderEmail);
        User receiver = userService.getUserByEmail(receiverEmail);

        // Validation
        validateTransaction(sender, receiver, amount);

        // Calcul des montants
        double fee = amount * FEE_PERCENTAGE;
        double totalAmount = amount + fee;

        // Mise à jour des soldes
        sender.setBalance(sender.getBalance() - totalAmount);
        receiver.setBalance(receiver.getBalance() + amount);

        // Création et sauvegarde
        Transaction transaction = Transaction.create(sender, receiver, amount, description, FEE_PERCENTAGE);
        return transactionRepository.save(transaction);
    }

    private void validateTransaction(User sender, User receiver, double amount) throws InsufficientBalanceException {
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("Sender or receiver cannot be null");
        }
        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("Cannot send money to yourself");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!sender.getConnections().contains(receiver)) {
            throw new IllegalStateException("You can only send money to your connections");
        }
        if (sender.getBalance() < (amount * (1 + FEE_PERCENTAGE))) {
            throw new InsufficientBalanceException("Insufficient balance including fee");
        }
    }

    @Transactional
    public List<Transaction> getUserTransactions(String email) {
        return transactionRepository.findAllByUserEmail(email);
    }
}
