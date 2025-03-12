package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
