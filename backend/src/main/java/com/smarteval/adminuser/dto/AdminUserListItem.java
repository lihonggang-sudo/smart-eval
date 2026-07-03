package com.smarteval.adminuser.dto;

import java.time.Instant;

public record AdminUserListItem(Long id, String username, String displayName,
                                boolean enabled, Instant createdAt) {
}
