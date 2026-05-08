package com.dudev.datingapp.security;

import com.dudev.datingapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String phone) {
        return userRepository.findByPhone(phone)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + phone));
    }

    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }

    private UserDetails toUserDetails(com.dudev.datingapp.user.entity.User user) {
        return User.withUsername(user.getId().toString())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
