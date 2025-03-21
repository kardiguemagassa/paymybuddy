package com.openclassrooms.paymybuddy.enttity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserConnectionId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "connection_id")
    private Long connectionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserConnectionId that = (UserConnectionId) o;
        return userId == that.userId && connectionId == that.connectionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, connectionId);
    }
}
