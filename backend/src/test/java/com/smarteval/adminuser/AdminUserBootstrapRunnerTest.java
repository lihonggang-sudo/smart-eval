package com.smarteval.adminuser;

import com.smarteval.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "app.bootstrap.admin.enabled=true",
        "app.bootstrap.admin.username=admin",
        "app.bootstrap.admin.password=secret",
        "app.bootstrap.admin.display-name=系统管理员"
})
class AdminUserBootstrapRunnerTest extends PostgresIntegrationTest {
    @Autowired
    private AdminUserBootstrapRunner bootstrapRunner;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDefaultAdminWhenNoAdminExists() {
        adminUserRepository.deleteAll();

        bootstrapRunner.run(null);

        AdminUser admin = adminUserRepository.findByUsername("admin").orElseThrow();
        Assertions.assertTrue(admin.isEnabled());
        Assertions.assertEquals("系统管理员", admin.getDisplayName());
        Assertions.assertTrue(passwordEncoder.matches("secret", admin.getPasswordHash()));
    }

    @Test
    void doesNotCreateDuplicateAdminWhenAdminExists() {
        adminUserRepository.deleteAll();
        bootstrapRunner.run(null);
        bootstrapRunner.run(null);

        Assertions.assertEquals(1, adminUserRepository.count());
    }
}
