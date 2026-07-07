package com.smarteval.evaluatoruser;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomCredentialGenerator {
    private static final char[] CREDENTIAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int USERNAME_LENGTH = 8;
    private static final int PASSWORD_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String username() {
        return randomString(USERNAME_LENGTH);
    }

    public String password() {
        return randomString(PASSWORD_LENGTH);
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CREDENTIAL_CHARS[secureRandom.nextInt(CREDENTIAL_CHARS.length)]);
        }
        return builder.toString();
    }
}
