package com.smarteval.evaluatee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluateeRequest(@NotBlank String name, @NotNull Long departmentId, boolean enabled) {
}
