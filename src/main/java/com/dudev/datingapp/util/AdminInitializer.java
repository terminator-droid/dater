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
    @Value("${app.admin.phone:+79000000000}")
    private String adminPhone;

    @Override
    public void run(String... args) {
        if (!userRepository.existsUserByName(adminUsername)) {
            User user = User.builder()
                    .password(passwordEncoder.encode(adminPassword))
                    .name(adminUsername)
                    .phone(adminPhone)
                    .birthDate(LocalDate.now().minusYears(30))
                    .googleSub("admin-google-sub")
                    .gender(Gender.MALE)
                    .build();
            userRepository.save(user);
        }
    }
}
