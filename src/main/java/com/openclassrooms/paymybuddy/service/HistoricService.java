package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.entity.Historic;
import com.openclassrooms.paymybuddy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoricService {

    User getUserByEmail(String email) ;
    Page<Historic> getUserHistoricPaginated(Long userId, Pageable pageable);

}
