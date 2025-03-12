package com.openclassrooms.paymybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    //https://fr.iban.com/currency-codes
    //https://www.baeldung.com/jpa-composite-primary-keys
    @GetMapping()
    public String home() {
        return "index";
    }
}
