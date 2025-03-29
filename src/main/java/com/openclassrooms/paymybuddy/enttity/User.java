package com.openclassrooms.paymybuddy.enttity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

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

    //transaction
    public void addSentTransaction(Transaction transaction) {
        this.sentTransactions.add(transaction);
        transaction.setSender(this);
    }

    public void addReceivedTransaction(Transaction transaction) {
        this.receivedTransactions.add(transaction);
        transaction.setReceiver(this);
    }

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
