package com.smarteval.result;

import com.smarteval.auth.AuthService;
import com.smarteval.common.BusinessException;
import com.smarteval.common.PageResponse;
import com.smarteval.evaluation.EvaluationGrade;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluationperiod.EvaluationPeriodContext;
import com.smarteval.result.dto.ResultDetailItem;
import com.smarteval.result.dto.ResultSummaryItem;
import com.smarteval.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultService {
    private final EvaluationRecordRepository evaluationRecordRepository;
    private final AuthService authService;

    public ResultService(EvaluationRecordRepository evaluationRecordRepository, AuthService authService) {
        this.evaluationRecordRepository = evaluationRecordRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResultSummaryItem> getSummary(
            CurrentUser currentUser,
            int page,
            int pageSize,
            Long departmentId,
            String evaluateeName
    ) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        var resultPage = evaluationRecordRepository.findResultSummaries(
                periodId,
                departmentId,
                normalizeTextFilter(evaluateeName),
                PageRequest.of(page - 1, pageSize)
        );
        return new PageResponse<>(resultPage.getContent(), resultPage.getTotalElements(), page, pageSize);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResultDetailItem> getDetails(
            CurrentUser currentUser,
            int page,
            int pageSize,
            Long departmentId,
            Long evaluateeId,
            Long evaluatorUserId,
            EvaluationGrade grade
    ) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        var resultPage = evaluationRecordRepository.findResultDetails(
                periodId,
                departmentId,
                evaluateeId,
                evaluatorUserId,
                grade,
                PageRequest.of(page - 1, pageSize)
        );
        return new PageResponse<>(
                resultPage.getContent().stream()
                        .map(record -> new ResultDetailItem(
                                record.getEvaluatorUser().getId(),
                                record.getEvaluatorUser().getUsername(),
                                record.getEvaluatorUser().getDepartment().getName(),
                                record.getEvaluatee().getId(),
                                record.getEvaluatee().getName(),
                                record.getGrade().name(),
                                record.getGrade().label(),
                                record.getComment1(),
                                record.getComment2(),
                                record.getCreatedAt().toInstant()
                        ))
                        .toList(),
                resultPage.getTotalElements(),
                page,
                pageSize
        );
    }

    private String normalizeTextFilter(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value;
    }
}
