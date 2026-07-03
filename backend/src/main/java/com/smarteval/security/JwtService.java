package com.smarteval.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final String issuer;
    private final long expiresInMinutes;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expires-in-minutes}") long expiresInMinutes
    ) {
        this.issuer = issuer;
        this.expiresInMinutes = expiresInMinutes;
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    public String issue(long userId, UserRole role, String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiresInMinutes, ChronoUnit.MINUTES);

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withClaim("uid", userId)
                .withClaim("role", role.name())
                .withClaim("username", username)
                .sign(algorithm);
    }

    public CurrentUser parse(String token) {
        var decodedJwt = verifier.verify(token);
        return new CurrentUser(
                decodedJwt.getClaim("uid").asLong(),
                UserRole.valueOf(decodedJwt.getClaim("role").asString()),
                decodedJwt.getClaim("username").asString()
        );
    }
}
