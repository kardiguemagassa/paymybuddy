package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
public class Users {

    @Id
    private long id;
    private String username;
    private String email;
    private String password;
}
