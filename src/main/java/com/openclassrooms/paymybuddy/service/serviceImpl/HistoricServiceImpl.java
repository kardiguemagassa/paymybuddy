package com.openclassrooms.paymybuddy.service.serviceImpl;

import com.openclassrooms.paymybuddy.entity.Historic;
import com.openclassrooms.paymybuddy.entity.User;
import com.openclassrooms.paymybuddy.repository.HistoricRepository;
import com.openclassrooms.paymybuddy.repository.UserRepository;
import com.openclassrooms.paymybuddy.service.HistoricService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class HistoricServiceImpl implements HistoricService {

    private final HistoricRepository historicRepository;
    private final UserRepository userRepository;

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("L'utilisateur n'est pas été trouvé"));
    }

    @Override
    public Page<Historic> getUserHistoricPaginated(Long userId, Pageable pageable) {
        return historicRepository.findUserHistoryPaginated(userId, pageable);
    }

}
