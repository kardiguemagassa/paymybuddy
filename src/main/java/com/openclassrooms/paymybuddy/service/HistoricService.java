package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Historic;
import com.openclassrooms.paymybuddy.enttity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoricService {

    User getUserByEmail(String email) ;
    Page<Historic> getUserHistoricPaginated(Long userId, Pageable pageable);

}
