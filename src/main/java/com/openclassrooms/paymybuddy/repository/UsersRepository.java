package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
