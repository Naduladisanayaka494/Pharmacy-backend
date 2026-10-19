package com.pharmacy.system.config;

import com.pharmacy.system.entity.Role;
import com.pharmacy.system.entity.User;
import com.pharmacy.system.entity.UserStatus;
import com.pharmacy.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.security.admin.full-name:Default System Administrator}")
    private String adminFullName;

    @Value("${app.security.admin.branch-id:1}")
    private Long defaultBranchId;

    @Override
    @Transactional

    public void run(String... args) {
        initializeSuperAdmin();
    }

    public void initializeSuperAdmin() {
        if(userRepository.existsByUsername(adminUsername)) {
            log.info("Super admin account '{}' already exists. Skipping initialization.", adminUsername);
            return;
        }

        User defaultAdmin = User.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(Role.ROLE_ADMIN)
                .branchId(defaultBranchId)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(defaultAdmin);
        log.info("Default super admin account '{}' initialized successfully.", adminUsername);
    }

}
