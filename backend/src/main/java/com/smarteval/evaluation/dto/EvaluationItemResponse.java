package com.smarteval.evaluation.dto;

import java.util.List;

public record EvaluationItemResponse(
        Long departmentId,
        String departmentName,
        boolean submitted,
        List<EvaluateeItem> items,
        List<GradeItem> grades
) {
}
