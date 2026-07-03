package com.smarteval.evaluatoruser.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchGenerateEvaluatorRequest(
        @NotNull Long departmentId,
        @Min(1) @Max(500) int count
) {
}
