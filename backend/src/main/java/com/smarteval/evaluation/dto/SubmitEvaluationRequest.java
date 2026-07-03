package com.smarteval.evaluation.dto;

import jakarta.validation.Valid;

import java.util.List;

public record SubmitEvaluationRequest(List<@Valid SubmitEvaluationRecord> records) {
}
