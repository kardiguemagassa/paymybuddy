package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="historic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Historic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long senderId;
    private String senderProfileName;

    private Long receiverId;
    private String receiverProfileName;

    private String description;
    private Double amount;
    private Double fee;
    private String currency;
    private LocalDateTime executionDate;

}
