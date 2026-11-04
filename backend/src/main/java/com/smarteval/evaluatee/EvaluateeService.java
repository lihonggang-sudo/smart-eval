package com.smarteval.evaluatee;

import com.smarteval.auth.AuthService;
import com.smarteval.common.BusinessException;
import com.smarteval.common.PageResponse;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluatee.dto.EvaluateeListItem;
import com.smarteval.evaluatee.dto.EvaluateeRequest;
import com.smarteval.evaluatee.dto.SetEnabledRequest;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluationperiod.EvaluationPeriodContext;
import com.smarteval.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluateeService {
    private static final String EVALUATEE_NOT_FOUND_MESSAGE = "被评价人不存在";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "部门不存在";

    private final EvaluateeRepository evaluateeRepository;
    private final DepartmentRepository departmentRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;
    private final AuthService authService;

    public EvaluateeService(
            EvaluateeRepository evaluateeRepository,
            DepartmentRepository departmentRepository,
            EvaluationRecordRepository evaluationRecordRepository,
            AuthService authService
    ) {
        this.evaluateeRepository = evaluateeRepository;
        this.departmentRepository = departmentRepository;
        this.evaluationRecordRepository = evaluationRecordRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EvaluateeListItem> list(
            CurrentUser currentUser,
            int page,
            int pageSize,
            Long departmentId,
            String name,
            Boolean enabled
    ) {
        authService.requireEnabledAdmin(currentUser);

        var evaluatees = evaluateeRepository.search(
                departmentId,
                name == null ? "" : name,
                enabled,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"))
        );
        return new PageResponse<>(
                evaluatees.getContent().stream().map(this::toListItem).toList(),
                evaluatees.getTotalElements(),
                page,
                pageSize
        );
    }

    @Transactional
    public EvaluateeListItem create(CurrentUser currentUser, EvaluateeRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        Department department = getRequiredDepartment(request.departmentId());
        Evaluatee evaluatee = new Evaluatee(request.name(), department, request.enabled());
        evaluatee.setEvaluationPeriodId(periodId);
        return toListItem(evaluateeRepository.save(evaluatee));
    }

    @Transactional
    public EvaluateeListItem update(CurrentUser currentUser, Long id, EvaluateeRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Evaluatee evaluatee = getRequiredEvaluatee(id);
        Department department = getRequiredDepartment(request.departmentId());
        evaluatee.update(request.name(), department, request.enabled());
        return toListItem(evaluatee);
    }

    @Transactional
    public EvaluateeListItem setEnabled(CurrentUser currentUser, Long id, SetEnabledRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Evaluatee evaluatee = getRequiredEvaluatee(id);
        evaluatee.updateEnabled(request.enabled());
        return toListItem(evaluatee);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long id) {
        authService.requireEnabledAdmin(currentUser);

        Evaluatee evaluatee = getRequiredEvaluatee(id);
        if (evaluationRecordRepository.existsByEvaluateeId(id)) {
            evaluatee.updateEnabled(false);
            return;
        }

        evaluateeRepository.delete(evaluatee);
    }

    private Evaluatee getRequiredEvaluatee(Long id) {
        return evaluateeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(EVALUATEE_NOT_FOUND_MESSAGE));
    }

    private Department getRequiredDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(DEPARTMENT_NOT_FOUND_MESSAGE));
    }

    private EvaluateeListItem toListItem(Evaluatee evaluatee) {
        return new EvaluateeListItem(
                evaluatee.getId(),
                evaluatee.getName(),
                evaluatee.getDepartment().getId(),
                evaluatee.getDepartment().getName(),
                evaluatee.isEnabled(),
                evaluatee.getCreatedAt().toInstant()
        );
    }
}
