package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.exception.UserNotFoundException;
import com.openclassrooms.paymybuddy.service.serviceImpl.TransactionServiceImpl;
import com.openclassrooms.utils.CurrencySymbols;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;
import static com.openclassrooms.utils.CurrencySymbols.SUPPORTED_CURRENCIES;

@Controller
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    @GetMapping
    public String showTransactionPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Model model) throws UserNotFoundException {

        String email = userDetails.getUsername();
        User currentUser = transactionService.getUserByTransactionEmail(email);

        // Création du Pageable
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Récupération paginée
        Page<Transaction> transactionsPage = transactionService.getUserTransactionsPaginated(email, pageable);
        Set<User> connections = currentUser.getConnections();

        // Ajout des attributs au modèle
        model.addAttribute("currentBalance", currentUser.getBalance());
        model.addAttribute("user", currentUser);
        model.addAttribute("transactions", transactionsPage);
        model.addAttribute("connections", connections);
        model.addAttribute("supportedCurrencies", SUPPORTED_CURRENCIES);
        model.addAttribute("currencySymbols", CurrencySymbols.SYMBOLS);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDirection", sortDirection);
        model.addAttribute("totalPages", transactionsPage.getTotalPages());

        return "transaction";
    }

    @GetMapping("/transactions")
    public String getTransactions(@AuthenticationPrincipal UserDetails userDetails, Model model) throws UserNotFoundException {
        User user = transactionService.getUserWithConnections(userDetails.getUsername());
        model.addAttribute("user", user);
        return "transaction";
    }

    @PostMapping
    public String makeTransaction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String receiverEmail,
            @RequestParam double amount,
            @RequestParam String description,
            @RequestParam String currency,
            RedirectAttributes redirectAttributes) {

        //validation stricte
        currency = currency.trim().toUpperCase();

        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException(String.format("Devise '%s' non supportée. Utilisez: %s", currency, String.join(", ", SUPPORTED_CURRENCIES)));
        }

        Transaction transaction = transactionService.makeTransaction(userDetails.getUsername(), receiverEmail, amount, currency, description);
        redirectAttributes.addFlashAttribute("success", String.format("Transfert réussi: %.2f %s (frais: %.2f %s)", amount, currency, transaction.getFee(), currency));
        return "redirect:/transaction";
    }
}
