package com.smarteval.evaluation;

import com.smarteval.common.ApiResponse;
import com.smarteval.evaluation.dto.EvaluationItemResponse;
import com.smarteval.evaluation.dto.SubmitEvaluationRequest;
import com.smarteval.evaluation.dto.SubmitEvaluationResponse;
import com.smarteval.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/api/mobile/evaluation/items")
    public ApiResponse<EvaluationItemResponse> items(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(evaluationService.getItems(currentUser));
    }

    @PostMapping("/api/mobile/evaluation/submit")
    public ApiResponse<SubmitEvaluationResponse> submit(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SubmitEvaluationRequest request
    ) {
        return ApiResponse.ok(evaluationService.submit(currentUser, request));
    }
}
