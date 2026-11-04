package com.smarteval.evaluatoruser;

import com.smarteval.auth.AuthService;
import com.smarteval.common.BusinessException;
import com.smarteval.common.PageResponse;
import com.smarteval.department.Department;
import com.smarteval.department.DepartmentRepository;
import com.smarteval.evaluationperiod.EvaluationPeriodContext;
import com.smarteval.evaluatoruser.dto.BatchGenerateEvaluatorRequest;
import com.smarteval.evaluatoruser.dto.BatchGenerateEvaluatorResponse;
import com.smarteval.evaluatoruser.dto.EvaluatorUserListItem;
import com.smarteval.evaluatoruser.dto.ResetEvaluatorPasswordResponse;
import com.smarteval.evaluatoruser.dto.SetEnabledRequest;
import com.smarteval.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EvaluatorUserService {
    private static final String DEPARTMENT_NOT_FOUND_MESSAGE = "部门不存在";
    private static final String EVALUATOR_USER_NOT_FOUND_MESSAGE = "考评用户不存在";
    private static final String USERNAME_GENERATION_FAILED_MESSAGE = "生成账号失败，请重试";
    private static final int USERNAME_GENERATION_ATTEMPT_MULTIPLIER = 20;
    private static final int MIN_USERNAME_GENERATION_ATTEMPTS = 100;

    private final EvaluatorUserRepository evaluatorUserRepository;
    private final DepartmentRepository departmentRepository;
    private final RandomCredentialGenerator randomCredentialGenerator;
    private final AuthService authService;

    public EvaluatorUserService(
            EvaluatorUserRepository evaluatorUserRepository,
            DepartmentRepository departmentRepository,
            RandomCredentialGenerator randomCredentialGenerator,
            AuthService authService
    ) {
        this.evaluatorUserRepository = evaluatorUserRepository;
        this.departmentRepository = departmentRepository;
        this.randomCredentialGenerator = randomCredentialGenerator;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EvaluatorUserListItem> list(
            CurrentUser currentUser,
            int page,
            int pageSize,
            Long departmentId,
            Boolean submitted,
            Boolean enabled
    ) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        var evaluatorUsers = evaluatorUserRepository.search(
                periodId,
                departmentId,
                submitted,
                enabled,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"))
        );
        return new PageResponse<>(
                evaluatorUsers.getContent().stream().map(this::toListItem).toList(),
                evaluatorUsers.getTotalElements(),
                page,
                pageSize
        );
    }

    @Transactional
    public BatchGenerateEvaluatorResponse batchGenerate(CurrentUser currentUser, BatchGenerateEvaluatorRequest request) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        Department department = getRequiredDepartment(request.departmentId());
        int maxAttempts = Math.max(MIN_USERNAME_GENERATION_ATTEMPTS,
                request.count() * USERNAME_GENERATION_ATTEMPT_MULTIPLIER);
        int attempts = 0;
        Set<String> usernamesInBatch = new HashSet<>();
        List<EvaluatorUser> createdUsers = new ArrayList<>();
        while (createdUsers.size() < request.count() && attempts < maxAttempts) {
            attempts++;
            String username = randomCredentialGenerator.username();
            if (usernamesInBatch.contains(username) || evaluatorUserRepository.existsByUsername(username)) {
                continue;
            }

            usernamesInBatch.add(username);
            EvaluatorUser user = new EvaluatorUser(
                    username,
                    randomCredentialGenerator.password(),
                    department,
                    true
            );
            user.setEvaluationPeriodId(periodId);
            createdUsers.add(user);
        }

        if (createdUsers.size() < request.count()) {
            throw new BusinessException(USERNAME_GENERATION_FAILED_MESSAGE);
        }

        List<EvaluatorUserListItem> items = evaluatorUserRepository.saveAll(createdUsers)
                .stream()
                .map(this::toListItem)
                .toList();
        return new BatchGenerateEvaluatorResponse(items.size(), items);
    }

    @Transactional
    public ResetEvaluatorPasswordResponse resetPassword(CurrentUser currentUser, Long id) {
        authService.requireEnabledAdmin(currentUser);

        EvaluatorUser evaluatorUser = getRequiredEvaluatorUser(id);
        String passwordPlain = randomCredentialGenerator.password();
        evaluatorUser.updatePasswordPlain(passwordPlain);
        return new ResetEvaluatorPasswordResponse(passwordPlain);
    }

    @Transactional
    public EvaluatorUserListItem setEnabled(CurrentUser currentUser, Long id, SetEnabledRequest request) {
        authService.requireEnabledAdmin(currentUser);

        EvaluatorUser evaluatorUser = getRequiredEvaluatorUser(id);
        evaluatorUser.updateEnabled(request.enabled());
        return toListItem(evaluatorUser);
    }

    @Transactional(readOnly = true)
    public List<EvaluatorUserListItem> listForPrint(
            CurrentUser currentUser,
            Long departmentId,
            Boolean submitted,
            Boolean enabled
    ) {
        authService.requireEnabledAdmin(currentUser);

        Long periodId = EvaluationPeriodContext.getCurrentPeriodId();
        if (periodId == null) {
            throw new BusinessException("当前批次未设置");
        }

        return evaluatorUserRepository.findForPrint(periodId, departmentId, submitted, enabled)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    private Department getRequiredDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(DEPARTMENT_NOT_FOUND_MESSAGE));
    }

    private EvaluatorUser getRequiredEvaluatorUser(Long id) {
        return evaluatorUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(EVALUATOR_USER_NOT_FOUND_MESSAGE));
    }

    private EvaluatorUserListItem toListItem(EvaluatorUser evaluatorUser) {
        return new EvaluatorUserListItem(
                evaluatorUser.getId(),
                evaluatorUser.getUsername(),
                evaluatorUser.getPasswordPlain(),
                evaluatorUser.getDepartment().getId(),
                evaluatorUser.getDepartment().getName(),
                evaluatorUser.isEnabled(),
                evaluatorUser.isSubmitted(),
                evaluatorUser.getSubmittedAt() == null ? null : evaluatorUser.getSubmittedAt().toInstant(),
                evaluatorUser.getCreatedAt().toInstant()
        );
    }
}
