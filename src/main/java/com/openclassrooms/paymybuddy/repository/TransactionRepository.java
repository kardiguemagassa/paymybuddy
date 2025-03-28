package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.enttity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.sender.email = :email OR t.receiver.email = :email ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserEmail(@Param("email") String email);

    @Query("SELECT t FROM Transaction t WHERE t.sender.email = :email ORDER BY t.createdAt DESC")
    List<Transaction> findSentTransactions(@Param("email") String email);

    @Query("SELECT t FROM Transaction t WHERE t.receiver.email = :email ORDER BY t.createdAt DESC")
    List<Transaction> findReceivedTransactions(@Param("email") String email);
}