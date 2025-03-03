package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", referencedColumnName = "id")
    private Long senderId;

    @ManyToOne
    @JoinColumn(name = "receiver_id", referencedColumnName = "id")
    private long receiverId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "description")
    private String description;
}
