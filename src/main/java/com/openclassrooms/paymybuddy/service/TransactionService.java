package com.openclassrooms.paymybuddy.service;


import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;

import java.util.List;

public interface TransactionService {

    List<Transaction> getUserTransactions(String email);
    Transaction makeTransaction(String senderEmail, String receiverEmail,
                                double amount, String description) throws UserNotFoundException, InsufficientBalanceException;
    User getUserWithConnections(String username) throws UserNotFoundException;
    User addBalance(String email, double amount) throws UserNotFoundException;
    User getUserByTransactionEmail(String email) throws UserNotFoundException;

}
