package com.smarteval.auth.dto;

public record EvaluatorMeResponse(Long id, String username, Long departmentId,
                                  String departmentName, boolean submitted) {
}
