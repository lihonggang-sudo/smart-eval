package com.smarteval.result;

import com.smarteval.common.ApiResponse;
import com.smarteval.common.PageResponse;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.result.dto.ResultDetailItem;
import com.smarteval.result.dto.ResultSummaryItem;
import com.smarteval.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResultController {
    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/api/admin/results/summary")
    public ApiResponse<PageResponse<ResultSummaryItem>> summary(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String evaluateeName
    ) {
        return ApiResponse.ok(resultService.getSummary(currentUser, page, pageSize, departmentId, evaluateeName));
    }

    @GetMapping("/api/admin/results/details")
    public ApiResponse<PageResponse<ResultDetailItem>> details(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long evaluateeId,
            @RequestParam(required = false) Long evaluatorUserId,
            @RequestParam(required = false) EvaluationGrade grade
    ) {
        return ApiResponse.ok(resultService.getDetails(
                currentUser, page, pageSize, departmentId, evaluateeId, evaluatorUserId, grade));
    }
}
