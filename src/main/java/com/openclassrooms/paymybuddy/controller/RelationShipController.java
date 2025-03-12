package com.openclassrooms.paymybuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RelationShipController {

    @GetMapping("/addRelationship")
    public String displayPage () {
        return "addRelationship";
    }
}
