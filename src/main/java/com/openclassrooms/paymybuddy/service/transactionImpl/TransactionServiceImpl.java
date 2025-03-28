package com.openclassrooms.paymybuddy.service.transactionImpl;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.TransactionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private static final double FEE_PERCENTAGE = 0.005;

    @Transactional
    @Override
    public List<Transaction> getUserTransactions(String email) {
        return transactionRepository.findAllByUserEmail(email);
    }

    @Transactional
    @Override
    public User getUserWithConnections(String email) throws UserNotFoundException {
        return userRepository.findWithConnectionsByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé"));
    }

    @Transactional
    @Override
    public User getUserByTransactionEmail(String email) throws UserNotFoundException {
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé avec l'email: " + email));
    }

    public User addBalance(String email, double amount) throws UserNotFoundException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        User user = getUserByTransactionEmail(email);

        user.setBalance(user.getBalance() + amount);
        return userRepository.save(user);
    }

    @Transactional
    @Override
    public Transaction makeTransaction(String senderEmail, String receiverEmail,
                                       double amount, String description) throws UserNotFoundException, InsufficientBalanceException {

        User sender = getUserByTransactionEmail(senderEmail);
        User receiver = getUserByTransactionEmail(receiverEmail);

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
            throw new IllegalArgumentException("Le destinataire ne peut pas être nul");
        }
        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("Impossible de vous envoyer de l'argent");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (!sender.getConnections().contains(receiver)) {
            throw new IllegalStateException("Vous ne pouvez envoyer de l'argent qu'à vos relations");
        }
        if (sender.getBalance() < (amount * (1 + FEE_PERCENTAGE))) {
            throw new InsufficientBalanceException("Solde insuffisant, frais compris");
        }
    }
}
