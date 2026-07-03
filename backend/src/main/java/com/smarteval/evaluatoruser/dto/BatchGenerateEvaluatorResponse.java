package com.smarteval.evaluatoruser.dto;

import java.util.List;

public record BatchGenerateEvaluatorResponse(int created, List<EvaluatorUserListItem> items) {
}
