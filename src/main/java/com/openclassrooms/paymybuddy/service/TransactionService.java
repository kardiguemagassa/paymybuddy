package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
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

    private Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<Transaction> getAllTransactionsForUser(String senderEmail) {
        return transactionRepository.findBySenderEmail(senderEmail);
    }

    @Transactional
    public void makeTransaction(Transaction transaction) {

        User sender = userRepository.findUserByEmail(transaction.getSender().getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        User receiver = userRepository.findUserByEmail(transaction.getReceiver().getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("Sender and Receiver emails must be different");
        }

        double moneyTransferPercentage = transaction.getAmount() * 0.005;

        double totalAmount = transaction.getAmount() + moneyTransferPercentage;

//        if (sender.getBalance() < totalAmount) {
//            throw new IllegalArgumentException("Sender does not have enough money");
//        }

        sender.setBalance(sender.getBalance() - totalAmount);
        receiver.setBalance(receiver.getBalance() + transaction.getAmount());

        userRepository.save(sender);
        userRepository.save(receiver);

        transaction.setExecutionDate(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}
