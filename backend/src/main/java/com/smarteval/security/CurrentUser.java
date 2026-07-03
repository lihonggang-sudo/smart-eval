package com.smarteval.security;

public record CurrentUser(long id, UserRole role, String username) {
}
