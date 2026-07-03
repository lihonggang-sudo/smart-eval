package com.smarteval.department.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(@NotBlank String name, int sortOrder) {
}
