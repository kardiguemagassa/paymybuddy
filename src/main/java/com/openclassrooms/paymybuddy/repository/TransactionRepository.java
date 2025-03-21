package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    //@Query(value = "select t from Transaction t where t.sender=:sender", nativeQuery = true)
    List<Transaction> findBySenderEmail(String senderEmail);



}
