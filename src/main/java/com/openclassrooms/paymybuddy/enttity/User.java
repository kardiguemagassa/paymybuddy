package com.openclassrooms.paymybuddy.enttity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="profile_name")
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String name;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit avoir au moins 8 caractères")
    @Pattern(regexp = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*",
            message = "Le mot de passe doit contenir 8 caractères minimum avec soit : 1 majuscule, " +
                    "1 minuscule et 1 chiffre ou 1 majuscule, 1 minuscule et 1 caractère spécial")
    private String password;

    @Column(name = "profile_image_url")
    private String profileImageUrl;
    private double balance;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "connection",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "connection_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"user_id", "connection_id"}
            )
    )
    private Set<User> connections = new HashSet<>();

    // bidirectionnelle
    @JsonIgnore
    @ManyToMany(mappedBy = "connections", fetch = FetchType.LAZY)
    private Set<User> connectedBy = new HashSet<>();

    // Transactions
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Transaction> sentTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Transaction> receivedTransactions = new ArrayList<>();


//    //transaction
//    public void addSentTransaction(Transaction transaction) {
//        this.sentTransactions.add(transaction);
//        transaction.setSender(this);
//    }
//
//    public void addReceivedTransaction(Transaction transaction) {
//        this.receivedTransactions.add(transaction);
//        transaction.setReceiver(this);
//    }


    //connection
    public void addConnection(User contact) {
        if (contact == null || this.equals(contact)) {
            throw new IllegalArgumentException("Invalid friend connection");
        }
        this.connections.add(contact);
        contact.getConnectedBy().add(this);
    }

    public void removeConnection(User contact) {
        this.connections.remove(contact);
        contact.getConnectedBy().remove(this);
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', email='" + email + "'}";
    }

    // UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
