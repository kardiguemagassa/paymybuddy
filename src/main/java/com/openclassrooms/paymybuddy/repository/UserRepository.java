package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findUserByUsername(String username);
    Optional<User> findUserByEmail(String email);

}