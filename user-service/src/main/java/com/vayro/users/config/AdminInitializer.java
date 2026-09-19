package com.vayro.users.config;

import com.vayro.users.entity.Role;
import com.vayro.users.entity.User;
import com.vayro.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstrap runner to initialize an admin account in development environments when explicitly enabled.
 */
@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.first-name:Vayro}")
    private String adminFirstName;

    @Value("${app.admin.last-name:Admin}")
    private String adminLastName;

    @Value("${app.admin.phone:+91 98200 11223}")
    private String adminPhone;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap initializer is disabled (ADMIN_BOOTSTRAP_ENABLED=false).");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin bootstrap is enabled but ADMIN_EMAIL or ADMIN_PASSWORD is empty. Skipping bootstrap account creation.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            User admin = new User();
            admin.setFirstName(adminFirstName != null && !adminFirstName.isBlank() ? adminFirstName.trim() : "Vayro");
            admin.setLastName(adminLastName != null && !adminLastName.isBlank() ? adminLastName.trim() : "Admin");
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhone(adminPhone != null && !adminPhone.isBlank() ? adminPhone.trim() : "+91 98200 11223");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);
            log.info("Admin bootstrap initialized account for: {}", normalizedEmail);
        } else {
            log.info("Admin account already exists for: {}", normalizedEmail);
        }
    }
}
