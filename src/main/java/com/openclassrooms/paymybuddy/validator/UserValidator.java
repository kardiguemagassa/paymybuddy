package com.openclassrooms.paymybuddy.validator;

import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


@Component
public class UserValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;

        if (user.getName() != null && user.getName().equalsIgnoreCase("admin")) {
            errors.rejectValue("name", "forbidden.name", "Ce nom d'utilisateur n'est pas autorisé");
        }

        if (user.getPassword() != null && !isPasswordValid(user.getPassword())) {
            errors.rejectValue("password", "invalid.password",
                    "Le mot de passe doit contenir : 1 majuscule, 1 minuscule et (1 chiffre OU 1 caractère spécial)");
        }
    }

    private boolean isPasswordValid(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d|.*[@#$%^&+=]).{8,}$");
    }
}
