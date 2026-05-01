package com.dudev.datingapp.util;


import com.dudev.datingapp.user.entity.Gender;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.admin.password:admin123}")
    private String adminPassword;
    @Value("${app.admin.username:admin123}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        if (!userRepository.existsUserByName(adminUsername)) {
            User user = User.builder()
                    .password(passwordEncoder.encode(adminUsername))
                    .name(adminPassword)
                    .phone("12")
                    .birthDate(LocalDate.now())
                    .googleSub("123")
                    .gender(Gender.MALE)
                    .build();
            userRepository.save(user);
        }
    }
}
