package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public Transaction sendMoney(String senderEmail, String receiverEmail, double amount, String description, String currency) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("Sender and Receiver emails must be different");
        }

        double fee = amount * 0.005;
        double totalAmount = amount + fee;

        if (sender.getBalance() < totalAmount) {
            throw new IllegalArgumentException("Sender does not have enough money");
        }

        sender.setBalance(sender.getBalance() - totalAmount);
        receiver.setBalance(receiver.getBalance() + amount);

        userRepository.save(sender);
        userRepository.save(receiver);

        Transaction transaction = new Transaction(sender, receiver, description, amount, new Timestamp(System.currentTimeMillis()).toLocalDateTime(), currency);
        return transactionRepository.save(transaction);
    }
}
