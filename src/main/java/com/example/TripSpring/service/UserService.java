package com.example.TripSpring.service;

import com.example.TripSpring.domain.user.User;
import com.example.TripSpring.exception.ResourceNotFoundException;
import com.example.TripSpring.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
}