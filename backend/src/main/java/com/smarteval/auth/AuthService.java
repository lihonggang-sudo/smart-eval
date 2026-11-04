package com.smarteval.auth;

import com.smarteval.adminuser.AdminUser;
import com.smarteval.adminuser.AdminUserRepository;
import com.smarteval.auth.dto.AdminMeResponse;
import com.smarteval.auth.dto.EvaluatorMeResponse;
import com.smarteval.auth.dto.LoginResponse;
import com.smarteval.common.BusinessException;
import com.smarteval.evaluationperiod.EvaluationPeriod;
import com.smarteval.evaluationperiod.EvaluationPeriodService;
import com.smarteval.evaluationperiod.dto.LatestPeriodInfo;
import com.smarteval.evaluatoruser.EvaluatorUser;
import com.smarteval.evaluatoruser.EvaluatorUserRepository;
import com.smarteval.security.CurrentUser;
import com.smarteval.security.JwtService;
import com.smarteval.security.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "用户名或密码错误";

    private final AdminUserRepository adminUserRepository;
    private final EvaluatorUserRepository evaluatorUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EvaluationPeriodService evaluationPeriodService;

    public AuthService(
            AdminUserRepository adminUserRepository,
            EvaluatorUserRepository evaluatorUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EvaluationPeriodService evaluationPeriodService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.evaluatorUserRepository = evaluatorUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.evaluationPeriodService = evaluationPeriodService;
    }

    @Transactional(readOnly = true)
    public LoginResponse adminLogin(String username, String password) {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .filter(AdminUser::isEnabled)
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS_MESSAGE));

        Optional<EvaluationPeriod> latestPeriod = evaluationPeriodService.getLatest();
        LatestPeriodInfo latestPeriodInfo = latestPeriod
                .map(p -> new LatestPeriodInfo(p.getId(), p.getName()))
                .orElse(null);

        return new LoginResponse(
                jwtService.issue(adminUser.getId(), UserRole.ADMIN, adminUser.getUsername()),
                toAdminMeResponse(adminUser),
                latestPeriodInfo
        );
    }

    @Transactional(readOnly = true)
    public AdminMeResponse getAdminMe(CurrentUser currentUser) {
        return toAdminMeResponse(requireEnabledAdmin(currentUser));
    }

    @Transactional(readOnly = true)
    public LoginResponse evaluatorLogin(String username, String password) {
        EvaluatorUser evaluatorUser = evaluatorUserRepository.findByUsername(username)
                .filter(EvaluatorUser::isEnabled)
                .filter(user -> user.getPasswordPlain().equals(password))
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS_MESSAGE));

        return new LoginResponse(
                jwtService.issue(evaluatorUser.getId(), UserRole.EVALUATOR, evaluatorUser.getUsername()),
                toEvaluatorMeResponse(evaluatorUser),
                null
        );
    }

    @Transactional(readOnly = true)
    public EvaluatorMeResponse getEvaluatorMe(CurrentUser currentUser) {
        return toEvaluatorMeResponse(requireEnabledEvaluator(currentUser));
    }

    @Transactional(readOnly = true)
    public AdminUser requireEnabledAdmin(CurrentUser currentUser) {
        return adminUserRepository.findById(currentUser.id())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS_MESSAGE));
    }

    @Transactional(readOnly = true)
    public EvaluatorUser requireEnabledEvaluator(CurrentUser currentUser) {
        return evaluatorUserRepository.findById(currentUser.id())
                .filter(EvaluatorUser::isEnabled)
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS_MESSAGE));
    }

    private AdminMeResponse toAdminMeResponse(AdminUser adminUser) {
        return new AdminMeResponse(adminUser.getId(), adminUser.getUsername(), adminUser.getDisplayName());
    }

    private EvaluatorMeResponse toEvaluatorMeResponse(EvaluatorUser evaluatorUser) {
        return new EvaluatorMeResponse(
                evaluatorUser.getId(),
                evaluatorUser.getUsername(),
                evaluatorUser.getDepartment().getId(),
                evaluatorUser.getDepartment().getName(),
                evaluatorUser.isSubmitted()
        );
    }
}
