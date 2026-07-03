package com.smarteval.evaluation.dto;

import java.time.Instant;

public record SubmitEvaluationResponse(boolean submitted, Instant submittedAt) {
}
