package com.smarteval.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    @Test
    void issuesAndParsesRoleScopedToken() {
        JwtService jwtService = new JwtService("test-issuer", "01234567890123456789012345678901", 60);

        String token = jwtService.issue(7L, UserRole.ADMIN, "admin");
        CurrentUser user = jwtService.parse(token);

        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.role()).isEqualTo(UserRole.ADMIN);
        assertThat(user.username()).isEqualTo("admin");
    }

    @Test
    void rejectsMalformedToken() {
        JwtService jwtService = new JwtService("test-issuer", "01234567890123456789012345678901", 60);

        assertThatThrownBy(() -> jwtService.parse("not-a-jwt"))
                .isInstanceOf(JWTVerificationException.class);
    }
}
