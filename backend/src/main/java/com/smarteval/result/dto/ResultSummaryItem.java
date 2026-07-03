package com.smarteval.result.dto;

public record ResultSummaryItem(Long evaluateeId, String evaluateeName, String departmentName,
                                long excellentCount, long competentCount,
                                long basicCompetentCount, long incompetentCount,
                                long totalCount) {
}
