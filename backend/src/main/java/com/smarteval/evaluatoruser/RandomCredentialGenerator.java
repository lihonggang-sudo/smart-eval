package com.smarteval.evaluatoruser;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
public class RandomCredentialGenerator {
    private static final char[] PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    public List<String> usernames(String departmentCode, int count) {
        List<String> usernames = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            usernames.add("%s-%04d".formatted(departmentCode, index));
        }
        return usernames;
    }

    public String password() {
        StringBuilder builder = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            builder.append(PASSWORD_CHARS[secureRandom.nextInt(PASSWORD_CHARS.length)]);
        }
        return builder.toString();
    }
}
