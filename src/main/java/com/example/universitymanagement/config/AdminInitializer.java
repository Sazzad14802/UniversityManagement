
package com.example.universitymanagement.config;

import com.example.universitymanagement.model.Role;
import com.example.universitymanagement.model.User;
import com.example.universitymanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (!userRepository.existsByEmail("admin@example.com")) {
                User admin = new User(
                        "admin@example.com",
                        passwordEncoder.encode("123"),
                        Role.ADMIN
                );

                userRepository.save(admin);

                System.out.println("Initial admin account created.");
            }
        };
    }
}
