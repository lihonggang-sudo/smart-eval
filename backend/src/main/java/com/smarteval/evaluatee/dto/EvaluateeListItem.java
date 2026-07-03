package com.smarteval.evaluatee.dto;

import java.time.Instant;

public record EvaluateeListItem(
        Long id,
        String name,
        Long departmentId,
        String departmentName,
        boolean enabled,
        Instant createdAt
) {
}
