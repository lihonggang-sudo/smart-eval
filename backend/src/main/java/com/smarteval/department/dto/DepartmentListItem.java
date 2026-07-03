package com.smarteval.department.dto;

import java.time.Instant;

public record DepartmentListItem(Long id, String name, int sortOrder, Instant createdAt) {
}
