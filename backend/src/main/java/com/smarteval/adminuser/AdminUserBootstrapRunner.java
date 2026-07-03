package com.smarteval.adminuser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserBootstrapRunner implements ApplicationRunner {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminUserBootstrapRunner(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.password:}") String password,
            @Value("${app.bootstrap.admin.display-name:系统管理员}") String displayName
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || adminUserRepository.count() > 0) {
            return;
        }
        validateProperties();

        adminUserRepository.save(new AdminUser(
                username,
                passwordEncoder.encode(password),
                displayName,
                true
        ));
    }

    private void validateProperties() {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(displayName)) {
            throw new IllegalStateException(
                    "Bootstrap admin is enabled, but username, password, and display-name must all be configured"
            );
        }
    }
}
