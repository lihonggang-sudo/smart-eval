package com.smarteval.result.dto;

import java.time.Instant;

public record ResultDetailItem(Long evaluatorUserId, String evaluatorUsername,
                               String evaluatorDepartmentName, Long evaluateeId,
                               String evaluateeName, String grade, String gradeLabel,
                               String comment1, String comment2, Instant createdAt) {
}
