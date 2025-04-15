package com.openclassrooms.paymybuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    @Pattern(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\."
            + "(com|fr|net|org|gov|edu|io|co|uk|de|it|es|be|lu|ch|ca|eu))$",
            message = "L'email doit utiliser une extension valide (.com, .fr, etc.)")
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

    @Transient
    private Double temporaryAmountAdded;

    //connection
    public void addConnection(User contact) {
        if (contact == null || this.equals(contact)) {
            throw new IllegalArgumentException("L'utilisateur n'est pas connecter");
        }
        this.connections.add(contact);
        contact.getConnectedBy().add(this);
    }

    public void removeConnection(User contact) {
        this.connections.remove(contact);
        contact.getConnectedBy().remove(this);
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
}
