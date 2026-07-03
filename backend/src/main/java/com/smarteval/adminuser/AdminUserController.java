package com.smarteval.adminuser;

import com.smarteval.adminuser.dto.AdminUserListItem;
import com.smarteval.adminuser.dto.CreateAdminUserRequest;
import com.smarteval.adminuser.dto.ResetPasswordRequest;
import com.smarteval.adminuser.dto.SetEnabledRequest;
import com.smarteval.adminuser.dto.UpdateAdminUserRequest;
import com.smarteval.common.ApiResponse;
import com.smarteval.common.PageResponse;
import com.smarteval.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/api/admin/admin-users")
    public ApiResponse<PageResponse<AdminUserListItem>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(adminUserService.list(currentUser, page, pageSize));
    }

    @PostMapping("/api/admin/admin-users")
    public ApiResponse<AdminUserListItem> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateAdminUserRequest request
    ) {
        return ApiResponse.ok(adminUserService.create(currentUser, request));
    }

    @PutMapping("/api/admin/admin-users/{id}")
    public ApiResponse<AdminUserListItem> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminUserRequest request
    ) {
        return ApiResponse.ok(adminUserService.update(currentUser, id, request));
    }

    @PutMapping("/api/admin/admin-users/{id}/enabled")
    public ApiResponse<AdminUserListItem> setEnabled(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @RequestBody SetEnabledRequest request
    ) {
        return ApiResponse.ok(adminUserService.setEnabled(currentUser, id, request));
    }

    @PostMapping("/api/admin/admin-users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        adminUserService.resetPassword(currentUser, id, request);
        return ApiResponse.ok(null);
    }
}
