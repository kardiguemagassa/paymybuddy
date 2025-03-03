
package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {

    @EmbeddedId
    private UserConnectionId userConnectionId;

    @ManyToOne
    @MapsId("userId")  // lie à la clé user_id de l'EmbeddedId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Main user in the connection

    @ManyToOne
    @MapsId("connectionId")  // lie à la clé connection_id de l'EmbeddedId
    @JoinColumn(name = "connection_id", nullable = false)
    private User connection;  // Logged in user

}
