package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Recherche users non encore connecté ou en base à user courant
    @Query("SELECT u FROM User u WHERE u.email != :userEmail " +
            "AND u.id NOT IN (SELECT c.id FROM User u JOIN u.connections c WHERE u.id = :userId)")
    List<User> findPotentialConnections(@Param("userEmail") String userEmail, @Param("userId") Long userId);

    // Set des relations d'un user connections + connectedBy
    @Query("SELECT DISTINCT u FROM User user " +
            "LEFT JOIN user.connections c " +
            "LEFT JOIN user.connectedBy cb " +
            "JOIN User u ON (u.id = c.id OR u.id = cb.id) " +
            "WHERE user.email = :email AND u.id != user.id")
    Set<User> findRelationsByEmail(@Param("email") String email);

    //transaction
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.connections WHERE u.email = :email")
    Optional<User> findWithConnectionsByEmail(@Param("email") String email);


}