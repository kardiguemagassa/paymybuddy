package com.openclassrooms.paymybuddy.controller;

import com.openclassrooms.paymybuddy.enttity.Historic;
import com.openclassrooms.paymybuddy.enttity.User;
import com.openclassrooms.paymybuddy.service.HistoricService;
import com.openclassrooms.utils.CurrencySymbols;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.data.domain.Pageable;

@Controller
@RequiredArgsConstructor
@RequestMapping("/historic")
public class HistoricController {

    private final HistoricService historicService;

    @GetMapping()
    public String showHistoric(@AuthenticationPrincipal UserDetails userDetails, Model model,
                               @PageableDefault(size = 10) Pageable pageable) {

        String email = userDetails.getUsername();
        User user = historicService.getUserByEmail(email);

        Page<Historic> historics = historicService.getUserHistoricPaginated(user.getId(), pageable);

        model.addAttribute("historics", historics);
        model.addAttribute("user", user);
        model.addAttribute("currencySymbols", CurrencySymbols.SYMBOLS);
        model.addAttribute("userId", user.getId());

        return "historic";
    }

}
