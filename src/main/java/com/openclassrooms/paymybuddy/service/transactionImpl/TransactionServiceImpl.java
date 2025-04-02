package com.openclassrooms.paymybuddy.service.transactionImpl;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.TransactionService;
import com.openclassrooms.paymybuddy.service.currencyServiceImpl.CurrencyServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyServiceImpl currencyService;
    private static final double FEE_PERCENTAGE = 0.005;

//    @Transactional
//    @Override
//    public List<Transaction> getUserTransactions(String email) {
//        return transactionRepository.findAllByUserEmail(email);
//    }

    @Override
    public Page<Transaction> getUserTransactionsPaginated(String email, Pageable pageable) {
        return transactionRepository.findBySenderEmailOrReceiverEmail(email, email, pageable);
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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé avec l'email: " + email));
    }

    @Override
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
                                       double amount, String transactionCurrency,
                                       String description)
            throws UserNotFoundException, InsufficientBalanceException {

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("Expéditeur non trouvé"));
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new UserNotFoundException("Destinataire non trouvé"));


        validateTransaction(sender, receiver, amount, transactionCurrency);

        // 3. Conversion et calcul des frais
        double amountInEur = currencyService.convertToEur(amount, transactionCurrency);
        double feeInEur = amountInEur * FEE_PERCENTAGE;
        double totalInEur = amountInEur + feeInEur;

        // 4. Mise à jour des soldes
        sender.setBalance(sender.getBalance() - totalInEur);
        receiver.setBalance(receiver.getBalance() + amountInEur);

        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setFee(feeInEur);
        transaction.setCurrency(transactionCurrency);
        transaction.setDescription(description);

        userRepository.saveAll(List.of(sender, receiver));
        return transactionRepository.save(transaction);
    }

    private void validateTransaction(User sender, User receiver, double amount, String currency)
            throws InsufficientBalanceException {

        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("L'expéditeur et le destinataire doivent être spécifiés");
        }
        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("Impossible d'envoyer de l'argent à soi-même");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        if (!sender.getConnections().contains(receiver)) {
            throw new IllegalStateException("Vous ne pouvez envoyer de l'argent qu'à vos relations");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Devise invalide");
        }

        // Conversion pour vérification du solde
        double amountInEur = currencyService.convertToEur(amount, currency);
        double totalWithFees = amountInEur * (1 + FEE_PERCENTAGE);

        if (sender.getBalance() < totalWithFees) {
            throw new InsufficientBalanceException(
                    String.format("Solde insuffisant. Nécessaire: %.2f EUR (%.2f %s + %.2f EUR de frais)",
                            totalWithFees,
                            amount,
                            currency,
                            amountInEur * FEE_PERCENTAGE)
            );
        }
    }

}
