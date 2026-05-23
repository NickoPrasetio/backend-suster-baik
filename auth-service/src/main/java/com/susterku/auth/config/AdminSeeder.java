package com.susterku.auth.config;

import com.susterku.auth.entity.UserEntity;
import com.susterku.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@susterku.com")) {
            UserEntity admin = UserEntity.builder()
                    .name("Admin SusterKu")
                    .email("admin@susterku.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("081200000000")
                    .role("ROLE_ADMIN")
                    .build();
            userRepository.save(admin);
            log.info("Admin account created: admin@susterku.com");
        }
    }
}
