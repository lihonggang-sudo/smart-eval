package com.smarteval.evaluation.dto;

import com.smarteval.evaluation.EvaluationGrade;
import jakarta.validation.constraints.Size;

public record SubmitEvaluationRecord(
        Long evaluateeId,
        EvaluationGrade grade,
        @Size(max = 255)
        String comment1,
        @Size(max = 255)
        String comment2
) {
}
