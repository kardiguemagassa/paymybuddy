package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String description;

    private double amount;

    @Column(name = "execution_date")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime executionDate;

    private String currency;

    public Transaction(User sender, User receiver, String description, double amount, LocalDateTime localDateTime, String currency) {
        this.sender = sender;
        this.receiver = receiver;
        this.description = description;
        this.amount = amount;
        this.executionDate = localDateTime;
        this.currency = currency;
    }
}
