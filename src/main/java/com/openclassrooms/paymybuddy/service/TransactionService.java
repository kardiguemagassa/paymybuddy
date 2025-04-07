package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    Page<Transaction> getUserTransactionsPaginated(String email, Pageable pageable);
    Transaction makeTransaction(String senderEmail, String receiverEmail,
                                double amount, String description, String currency) throws UserNotFoundException, InsufficientBalanceException;
    User getUserWithConnections(String username) throws UserNotFoundException;
    User addBalance(String email, Double amount, String randomAmount) throws UserNotFoundException;
    User getUserByTransactionEmail(String email) throws UserNotFoundException;

}
