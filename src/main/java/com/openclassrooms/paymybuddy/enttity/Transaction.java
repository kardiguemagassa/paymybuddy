package com.openclassrooms.paymybuddy.enttity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
//@ToString(exclude = {"sender", "receiver"})
//@EqualsAndHashCode(exclude = {"sender", "receiver"})
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @JsonIgnore
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    @JsonIgnore
    private User receiver;

    private String description;
    private double amount;
    private double fee;

    @Column(name = "execution_date", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String currency = "EUR";

    public static Transaction create(User sender, User receiver, double amount, String description, double feePercentage) {
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setFee(amount * feePercentage);
        transaction.setDescription(description);
        return transaction;
    }
}