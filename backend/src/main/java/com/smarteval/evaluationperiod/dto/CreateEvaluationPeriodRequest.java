package com.smarteval.evaluationperiod.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEvaluationPeriodRequest(
        @NotBlank(message = "批次名称不能为空")
        @Size(max = 255, message = "批次名称不能超过255个字符")
        String name
) {
}
