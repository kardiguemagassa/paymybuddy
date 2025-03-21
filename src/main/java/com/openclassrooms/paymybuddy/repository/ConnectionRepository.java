package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.Connection;
import com.openclassrooms.paymybuddy.enttity.UserConnectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, UserConnectionId> {

    @Query("SELECT c FROM Connection c WHERE c.userConnectionId.userId = :userId OR c.userConnectionId.connectionId = :userId")
    List<Connection> findConnectionsByUser(@Param("userId") Long userId);


}
