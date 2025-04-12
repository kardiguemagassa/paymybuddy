package com.openclassrooms.paymybuddy.repository;

import com.openclassrooms.paymybuddy.entity.Historic;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

@Repository
public interface HistoricRepository extends JpaRepository<Historic, Long> {

    @Query("SELECT h FROM Historic h WHERE h.senderId = :userId OR h.receiverId = :userId ORDER BY h.executionDate DESC")
    Page<Historic> findUserHistoryPaginated(@Param("userId") Long userId, Pageable pageable);
}
