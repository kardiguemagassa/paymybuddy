package com.openclassrooms.paymybuddy.exception;

import com.openclassrooms.paymybuddy.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // gestion de spring validation sur entity
    // Cette méthode est appelée quand une validation échoue sur une entité persistée (@Entity) avec @NotNull, @Email, etc.
    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(ConstraintViolationException e, RedirectAttributes redirectAttributes) {
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> {
                            String path = violation.getPropertyPath().toString();
                            return path.substring(path.lastIndexOf('.') + 1);}, //  Extrait seulement le nom du champ email dans entity user
                        // Spring voit que l'exception a été traitée et logue automatiquement le message :
                        // Resolved [jakarta.validation.ConstraintViolationException:
                        // registerUser.user.email: L'email doit utiliser une extension valide (.com, .fr, etc.)]

                        ConstraintViolation::getMessage, (msg1, msg2) -> msg1 + ", " + msg2)); // Si plusieurs erreurs sur le même champ, elles sont concaténées avec ", ".

        redirectAttributes.addFlashAttribute("errors", errors);
        redirectAttributes.addFlashAttribute("user", getTargetFromViolations(e.getConstraintViolations())); // On ajoute l'objet en erreur pour préremplir le formulaire après la redirection.

        return "redirect:/register";
    }

    // gestion de spring validation sur entity
    private User getTargetFromViolations(Set<ConstraintViolation<?>> violations) {
        return (User) violations.stream().findFirst().map(ConstraintViolation::getLeafBean).orElse(null);
        /*
        * findFirst() : Prend la première erreur.

        .map(ConstraintViolation::getLeafBean) : Récupère l'objet concerné (User).

        orElse(null) : Retourne null si aucune erreur.*/
    }

//    private User getTargetFromViolations(Set<ConstraintViolation<?>> violations) {
//        return violations.stream()
//                .findFirst()
//                .map(ConstraintViolation::getLeafBean)
//                .filter(bean -> bean instanceof User) // Vérifie que c'est bien un User
//                .map(bean -> (User) bean) // Cast seulement si c'est un User
//                .orElse(null);
//    }

    // Cette méthode est appelée dans le contrôleur Spring utilisant @Valid.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationExceptions(MethodArgumentNotValidException e, RedirectAttributes redirectAttributes) {

        Map<String, String> errors = e.getBindingResult().getFieldErrors()
                .stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (msg1, msg2) -> msg1 + ", " + msg2)); // Récupère toutes les erreurs des champs invalides.

        redirectAttributes.addFlashAttribute("fieldErrors", errors);
        redirectAttributes.addFlashAttribute("user", e.getBindingResult().getTarget());
        return "redirect:/register";
    }

    // Méthode unifiée pour gérer IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentExceptions(IllegalArgumentException e, RedirectAttributes redirectAttributes,
                                                  HttpServletRequest request) {

        LOGGER.warn("Validation error: {}", e.getMessage());

        String referer = request.getHeader("referer");

        if (referer != null || !referer.contains("addRelationship")) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/addRelationship";
        }

         // erreur de balance
        if (e.getMessage().contains("balance") || e.getMessage().contains("montant") || e.getMessage().contains("amount")) {
            redirectAttributes.addFlashAttribute("errorBalance", e.getMessage());
            return "redirect:/addBalance";
        }

        String redirectUrl = determineRedirectUrl(e);
        String errorAttribute = determineErrorAttribute(e);

        redirectAttributes.addFlashAttribute(errorAttribute, e.getMessage());
        return redirectUrl;
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeExceptions(RuntimeException e, RedirectAttributes redirectAttributes,
                                          HttpServletRequest request) {
        LOGGER.error("Erreur système: {}", e.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/addRelationship")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Une erreur est survenue lors de l'opération");
            return "redirect:/addRelationship";
        }

        redirectAttributes.addFlashAttribute("error", "Une erreur système est survenue");
        return "redirect:/error";
    }



    private String determineRedirectUrl(IllegalArgumentException e) {
        if (e.getMessage().contains("password")) {
            return "redirect:/profile#password";
        }  else if (e.getMessage().contains("email") || e.getMessage().contains("profile")) {
            return "redirect:/profile";
        }
        return "redirect:/register";
    }

    private String determineErrorAttribute(IllegalArgumentException e) {
        if (e.getMessage().contains("password")) {
            return "errorPassword";
        } else if (e.getMessage().contains("email") || e.getMessage().contains("profile")) {
            return "errorProfile";
        } else if (e.getMessage().contains("balance") || e.getMessage().contains("montant")) {
            return "errorBalance";
        }
        return "error";
    }

    // Gestion spécifique pour UserNotFoundException
    @ExceptionHandler(UserNotFoundException.class)
    public String handleUserNotFoundException(UserNotFoundException e, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        LOGGER.error("User not found error: {}", e.getMessage());

        // redirection vers des pages specifique
        String referer = request.getHeader("Referer");

        if (referer != null && referer.contains("/addBalance")) {
            redirectAttributes.addFlashAttribute("errorBalance", "Utilisateur non trouvé: " + e.getMessage());
            return "redirect:/addBalance";
        } else if (referer != null && referer.contains("/transaction")) {
            redirectAttributes.addFlashAttribute("errorTransaction", e.getMessage());
            return "redirect:/transaction";
        }

        redirectAttributes.addFlashAttribute("errorPassword", "Session expirée");
        return "redirect:/login";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception e, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        LOGGER.error("Unexpected error", e);

        //
        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }

        redirectAttributes.addFlashAttribute("error", "Une erreur inattendue est survenue");
        return "redirect:/error";
    }

    @ExceptionHandler(EmailExistException.class)
    public String handleEmailExists(EmailExistException e, RedirectAttributes redirectAttributes) {
        LOGGER.warn("Email exist: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorProfile", e.getMessage());
        return "redirect:/profile";
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public String handlePasswordMismatch(PasswordMismatchException e, RedirectAttributes redirectAttributes) {
        LOGGER.warn("Password mismatch: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorPassword", e.getMessage());
        return "redirect:/profile#password";
    }

    @ExceptionHandler(IOException.class)
    public String handleIOException(IOException e, RedirectAttributes redirectAttributes) {
        LOGGER.warn("IOException: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorProfile", "Erreur lors du traitement de l'image");
        return "redirect:/profile";
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public String handleInvalidPassword(InvalidPasswordException e, RedirectAttributes redirectAttributes) {
        LOGGER.warn("Invalid password: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorPassword", e.getMessage());
        return "redirect:/profile#password";
    }

    @ExceptionHandler(SecurityContextUpdateException.class)
    public String handleSecurityContextUpdate(SecurityContextUpdateException e, RedirectAttributes redirectAttributes) {
        LOGGER.error("Security context update failed", e);
        redirectAttributes.addFlashAttribute("error", "Problème d'authentification");
        return "redirect:/login";
    }
}