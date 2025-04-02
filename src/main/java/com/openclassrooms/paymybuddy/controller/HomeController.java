package com.openclassrooms.paymybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    //https://fr.iban.com/currency-codes
    //https://www.baeldung.com/jpa-composite-primary-keys
    // https://docs.spring.io/spring-data/data-commons/docs/1.6.1.RELEASE/reference/html/repositories.html
    // https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html

    @GetMapping("/")
    public String showHomePage() {
        return "index";
    }
}
