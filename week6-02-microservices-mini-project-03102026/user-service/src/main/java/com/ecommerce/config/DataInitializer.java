package com.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Role;
import com.ecommerce.service.CustomerService;

import lombok.RequiredArgsConstructor;

/**
 * Seeds a default admin account on every startup (idempotent — skips if already exists).
 *
 * Credentials:
 *   username : admin
 *   password : Admin@123
 *   role     : ADMIN
 *
 * Change the password immediately in production via the admin management API.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (customerService.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
            log.info("Default admin account already exists — skipping seed.");
            return;
        }

        Customer admin = new Customer();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        admin.setFirstName("Super");
        admin.setLastName("Admin");
        admin.setMobileNumber("9000000000");
        admin.setAccountEnabled(true);

        customerService.save(admin);
        log.info("Default admin created — username: '{}', password: '{}'", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }
}
