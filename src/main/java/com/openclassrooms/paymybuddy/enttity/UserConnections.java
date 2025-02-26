package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.*;

@Entity
@Table()
public class UserConnections {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userId;
    private long connectionId;
}
