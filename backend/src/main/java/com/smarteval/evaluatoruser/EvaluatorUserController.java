package com.smarteval.evaluatoruser;

import com.smarteval.common.ApiResponse;
import com.smarteval.common.PageResponse;
import com.smarteval.evaluatoruser.dto.BatchGenerateEvaluatorRequest;
import com.smarteval.evaluatoruser.dto.BatchGenerateEvaluatorResponse;
import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import com.smarteval.evaluatoruser.dto.ResetEvaluatorPasswordResponse;
import com.smarteval.evaluatoruser.dto.SetEnabledRequest;
import com.smarteval.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluatorUserController {
    private final EvaluatorUserService evaluatorUserService;

    public EvaluatorUserController(EvaluatorUserService evaluatorUserService) {
        this.evaluatorUserService = evaluatorUserService;
    }

    @GetMapping("/api/admin/evaluator-users")
    public ApiResponse<PageResponse<EvaluatorUserListItem>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean submitted,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(evaluatorUserService.list(currentUser, page, pageSize, departmentId, submitted, enabled));
    }

    @PostMapping("/api/admin/evaluator-users/batch-generate")
    public ApiResponse<BatchGenerateEvaluatorResponse> batchGenerate(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody BatchGenerateEvaluatorRequest request
    ) {
        return ApiResponse.ok(evaluatorUserService.batchGenerate(currentUser, request));
    }

    @PostMapping("/api/admin/evaluator-users/{id}/reset-password")
    public ApiResponse<ResetEvaluatorPasswordResponse> resetPassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(evaluatorUserService.resetPassword(currentUser, id));
    }

    @PutMapping("/api/admin/evaluator-users/{id}/enabled")
    public ApiResponse<EvaluatorUserListItem> setEnabled(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody SetEnabledRequest request
    ) {
        return ApiResponse.ok(evaluatorUserService.setEnabled(currentUser, id, request));
    }
}
