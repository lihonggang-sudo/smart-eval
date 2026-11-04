package com.smarteval.department;

import com.smarteval.auth.AuthService;
import com.smarteval.common.BusinessException;
import com.smarteval.common.PageResponse;
import com.smarteval.department.dto.DepartmentListItem;
import com.smarteval.department.dto.DepartmentRequest;
import com.smarteval.evaluation.EvaluationRecordRepository;
import com.smarteval.evaluatee.EvaluateeRepository;
import com.smarteval.evaluationperiod.EvaluationPeriodContext;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {
    private static final String DUPLICATE_NAME_MESSAGE = "部门名称已存在";
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "部门不存在";
    private static final String DEPARTMENT_DELETE_BLOCKED_MESSAGE = "部门下存在关联数据，不能删除";

    private final DepartmentRepository departmentRepository;
    private final EvaluateeRepository evaluateeRepository;
    private final EvaluatorUserRepository evaluatorUserRepository;
    private final EvaluationRecordRepository evaluationRecordRepository;
    private final AuthService authService;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            EvaluateeRepository evaluateeRepository,
            EvaluatorUserRepository evaluatorUserRepository,
            EvaluationRecordRepository evaluationRecordRepository,
            AuthService authService
    ) {
        this.departmentRepository = departmentRepository;
        this.evaluateeRepository = evaluateeRepository;
        this.evaluatorUserRepository = evaluatorUserRepository;
        this.evaluationRecordRepository = evaluationRecordRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentListItem> list(CurrentUser currentUser, int page, int pageSize, String name) {
        authService.requireEnabledAdmin(currentUser);

        var departments = departmentRepository.findByNameContaining(
                name == null ? "" : name,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder", "id"))
        );
        return new PageResponse<>(
                departments.getContent().stream().map(this::toListItem).toList(),
                departments.getTotalElements(),
                page,
                pageSize
        );
    }

    @Transactional
    public DepartmentListItem create(CurrentUser currentUser, DepartmentRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        if (departmentRepository.existsByName(request.name())) {
            throw new BusinessException(DUPLICATE_NAME_MESSAGE);
        }

        Department department = new Department(request.name(), request.sortOrder());
        department.setEvaluationPeriodId(periodId);
        return toListItem(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentListItem update(CurrentUser currentUser, Long id, DepartmentRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Department department = getRequired(id);
        if (departmentRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(DUPLICATE_NAME_MESSAGE);
        }

        department.update(request.name(), request.sortOrder());
        return toListItem(department);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long id) {
        authService.requireEnabledAdmin(currentUser);

        Department department = getRequired(id);
        if (evaluateeRepository.countByDepartmentId(id) > 0
                || evaluatorUserRepository.countByDepartmentId(id) > 0
                || evaluationRecordRepository.countByDepartmentId(id) > 0) {
            throw new BusinessException(DEPARTMENT_DELETE_BLOCKED_MESSAGE);
        }

        departmentRepository.delete(department);
    }

    private Department getRequired(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(DEPARTMENT_NOT_FOUND_MESSAGE));
    }

    private DepartmentListItem toListItem(Department department) {
        return new DepartmentListItem(
                department.getId(),
                department.getName(),
                department.getSortOrder(),
                department.getCreatedAt().toInstant()
        );
    }
}
