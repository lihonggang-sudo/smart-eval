package com.smarteval.adminuser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminUserBootstrapRunnerUnitTest {
    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void doesNotQueryOrCreateAdminWhenDisabled() {
        AdminUserBootstrapRunner runner = new AdminUserBootstrapRunner(
                adminUserRepository,
                passwordEncoder,
                false,
                "",
                "",
                "系统管理员"
        );

        runner.run(null);

        verifyNoInteractions(adminUserRepository, passwordEncoder);
    }

    @Test
    void rejectsEnabledBootstrapWithoutPasswordWhenNoAdminExists() {
        when(adminUserRepository.count()).thenReturn(0L);
        AdminUserBootstrapRunner runner = new AdminUserBootstrapRunner(
                adminUserRepository,
                passwordEncoder,
                true,
                "admin",
                "",
                "系统管理员"
        );

        Assertions.assertThrows(IllegalStateException.class, () -> runner.run(null));
        verify(adminUserRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
