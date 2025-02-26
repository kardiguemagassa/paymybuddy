package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Transactions {

    @Id
    private long id;
    private long senderId;
    private long receiverId;
    private double amount;
    private String description;

}
