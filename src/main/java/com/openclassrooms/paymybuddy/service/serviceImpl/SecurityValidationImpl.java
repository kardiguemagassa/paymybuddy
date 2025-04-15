package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.exception.InsufficientBalanceException;
import com.openclassrooms.paymybuddy.exception.InvalidPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SecurityValidationImpl  {

    private static final double FEE_PERCENTAGE = 0.005;
    private final CurrencyServiceImpl currencyService;
    private final CustomUserDetailsService customUserDetailsService;

    public void updateSecurityContext(User user, HttpServletRequest request) {
        UserDetails newUserDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(newUserDetails, newUserDetails.getPassword(), newUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // Rafraîchir la session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
    }

    public void validateTransaction(User sender, User receiver, double amount, String currency)
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

    public void validateEmail(String email) {
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException("L'email n'est pas valide");
        }
    }

    public void validatePassword(String password) throws InvalidPasswordException {

        if (password.length() < 8) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins 8 caractères");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins un caractère spécial");
        }

        if (!password.matches(".*\\d.*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins un chiffre");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins une majuscule");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins une minuscule");
        }
    }
}
