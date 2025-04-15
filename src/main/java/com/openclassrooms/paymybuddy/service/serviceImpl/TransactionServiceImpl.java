package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.entity.Transaction;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.repository.TransactionRepository;

import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.TransactionService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyServiceImpl currencyService;
    private final SecurityValidationImpl securityValidation;
    private final Random random = new Random();
    private static final double FEE_PERCENTAGE = 0.005;

    @Override
    public Page<Transaction> getUserTransactionsPaginated(String email, Pageable pageable) {
        return transactionRepository.findBySenderEmailOrReceiverEmail(email, email, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public User getUserWithConnections(String email) throws UserNotFoundException {
        return userRepository.findWithConnectionsByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé"));
    }

    @Transactional(readOnly = true)
    @Override
    public User getUserByTransactionEmail(String email) throws UserNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Aucun utilisateur trouvé avec l'email: " + email));
    }

    @Transactional
    @Override
    public User addBalance(String email, Double amount, String randomAmount) throws UserNotFoundException,IllegalArgumentException {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email est requis pour le rechargement du solde");
        }

        if (amount == null && (randomAmount == null || randomAmount.isBlank())) {
            throw new IllegalArgumentException("Veuillez spécifier un montant pour le rechargement");
        }

        User userBefore = getUserByTransactionEmail(email);
        double previousBalance = userBefore.getBalance();

        // Calcul et application du montant
        double amountToAdd = calculateAmountToAdd(amount, randomAmount);
        userBefore.setBalance(previousBalance + amountToAdd);
        User updatedUser = userRepository.save(userBefore);

        // Stocker le montant ajouté pour le message
        updatedUser.setTemporaryAmountAdded(amountToAdd);

        return updatedUser;
    }

    @Transactional(readOnly = true)
    public String getFormattedBalanceUpdateMessage(User user, Double amountOrNull) {
        double amountAdded = amountOrNull != null ? amountOrNull : user.getTemporaryAmountAdded();
        return String.format("Rechargement réussi ! %.2f € ajoutés. Nouveau solde: %.2f €", amountAdded, user.getBalance());
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


        securityValidation.validateTransaction(sender, receiver, amount, transactionCurrency);

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

    @Transactional
    public double calculateAmountToAdd(Double amount, String randomAmount) {
        if (randomAmount != null && randomAmount.equals("random")) {
            double randomValue = 10 + (2000 - 10) * random.nextDouble();
            return Math.round(randomValue * 100.0) / 100.0; // Arrondi à 2 décimales
        }

        if (amount != null && amount > 0) {
            return amount;
        }

        throw new IllegalArgumentException("Montant invalide");
    }

}
