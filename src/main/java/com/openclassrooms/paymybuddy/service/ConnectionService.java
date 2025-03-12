package com.openclassrooms.paymybuddy.service;

import com.openclassrooms.paymybuddy.enttity.Connection;
import com.openclassrooms.paymybuddy.repository.ConnectionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;

    public List<Connection> findAll() {
        return connectionRepository.findAll();
    }
}
