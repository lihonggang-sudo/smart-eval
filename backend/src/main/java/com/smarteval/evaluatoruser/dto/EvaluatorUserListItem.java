package com.smarteval.evaluatoruser.dto;

import java.time.Instant;

public record EvaluatorUserListItem(
        Long id,
        String username,
        String passwordPlain,
        Long departmentId,
        String departmentName,
        boolean enabled,
        boolean submitted,
        Instant submittedAt,
        Instant createdAt
) {
}
