package com.smarteval.evaluatee;

import com.smarteval.common.ApiResponse;
import com.smarteval.common.PageResponse;
import com.smarteval.evaluatee.dto.EvaluateeListItem;
import com.smarteval.evaluatee.dto.EvaluateeRequest;
import com.smarteval.evaluatee.dto.SetEnabledRequest;
import com.smarteval.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvaluateeController {
    private final EvaluateeService evaluateeService;

    public EvaluateeController(EvaluateeService evaluateeService) {
        this.evaluateeService = evaluateeService;
    }

    @GetMapping("/api/admin/evaluatees")
    public ApiResponse<PageResponse<EvaluateeListItem>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "") String name,
            @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.ok(evaluateeService.list(currentUser, page, pageSize, departmentId, name, enabled));
    }

    @PostMapping("/api/admin/evaluatees")
    public ApiResponse<EvaluateeListItem> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody EvaluateeRequest request
    ) {
        return ApiResponse.ok(evaluateeService.create(currentUser, request));
    }

    @PutMapping("/api/admin/evaluatees/{id}")
    public ApiResponse<EvaluateeListItem> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody EvaluateeRequest request
    ) {
        return ApiResponse.ok(evaluateeService.update(currentUser, id, request));
    }

    @DeleteMapping("/api/admin/evaluatees/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        evaluateeService.delete(currentUser, id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/evaluatees/{id}/enabled")
    public ApiResponse<EvaluateeListItem> setEnabled(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody SetEnabledRequest request
    ) {
        return ApiResponse.ok(evaluateeService.setEnabled(currentUser, id, request));
    }
}
