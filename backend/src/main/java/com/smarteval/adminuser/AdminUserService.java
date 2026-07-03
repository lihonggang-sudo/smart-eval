package com.smarteval.adminuser;

import com.smarteval.auth.AuthService;
import com.smarteval.adminuser.dto.AdminUserListItem;
import com.smarteval.adminuser.dto.CreateAdminUserRequest;
import com.smarteval.adminuser.dto.ResetPasswordRequest;
import com.smarteval.adminuser.dto.SetEnabledRequest;
import com.smarteval.adminuser.dto.UpdateAdminUserRequest;
import com.smarteval.common.BusinessException;
import com.smarteval.common.PageResponse;
import com.smarteval.security.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final String DUPLICATE_USERNAME_MESSAGE = "用户名已存在";
    private static final String LAST_ENABLED_ADMIN_MESSAGE = "不能禁用最后一个启用的管理员";
    private static final String ADMIN_NOT_FOUND_MESSAGE = "管理员不存在";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public AdminUserService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserListItem> list(CurrentUser currentUser, int page, int pageSize) {
        authService.requireEnabledAdmin(currentUser);

        var adminUsers = adminUserRepository.findAll(
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"))
        );
        return new PageResponse<>(
                adminUsers.getContent().stream().map(this::toListItem).toList(),
                adminUsers.getTotalElements(),
                page,
                pageSize
        );
    }

    @Transactional
    public AdminUserListItem create(CurrentUser currentUser, CreateAdminUserRequest request) {
        authService.requireEnabledAdmin(currentUser);

        if (adminUserRepository.existsByUsername(request.username())) {
            throw new BusinessException(DUPLICATE_USERNAME_MESSAGE);
        }

        AdminUser adminUser = adminUserRepository.save(new AdminUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                true
        ));
        return toListItem(adminUser);
    }

    @Transactional
    public AdminUserListItem update(CurrentUser currentUser, Long id, UpdateAdminUserRequest request) {
        authService.requireEnabledAdmin(currentUser);

        AdminUser adminUser = getRequired(id);
        adminUser.updateDisplayName(request.displayName());
        return toListItem(adminUser);
    }

    @Transactional
    public AdminUserListItem setEnabled(CurrentUser currentUser, Long id, SetEnabledRequest request) {
        authService.requireEnabledAdmin(currentUser);

        AdminUser adminUser = getRequired(id);
        if (!request.enabled() && adminUser.isEnabled() && adminUserRepository.countByEnabledTrue() <= 1) {
            throw new BusinessException(LAST_ENABLED_ADMIN_MESSAGE);
        }

        adminUser.updateEnabled(request.enabled());
        return toListItem(adminUser);
    }

    @Transactional
    public void resetPassword(CurrentUser currentUser, Long id, ResetPasswordRequest request) {
        authService.requireEnabledAdmin(currentUser);

        AdminUser adminUser = getRequired(id);
        adminUser.updatePasswordHash(passwordEncoder.encode(request.password()));
    }

    private AdminUser getRequired(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ADMIN_NOT_FOUND_MESSAGE));
    }

    private AdminUserListItem toListItem(AdminUser adminUser) {
        return new AdminUserListItem(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.getDisplayName(),
                adminUser.isEnabled(),
                adminUser.getCreatedAt().toInstant()
        );
    }
}
