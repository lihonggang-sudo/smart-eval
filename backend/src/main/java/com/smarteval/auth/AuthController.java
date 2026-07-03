package com.smarteval.auth;

import com.smarteval.auth.dto.LoginRequest;
import com.smarteval.auth.dto.LoginResponse;
import com.smarteval.common.ApiResponse;
import com.smarteval.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/admin/auth/login")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.adminLogin(request.username(), request.password()));
    }

    @GetMapping("/api/admin/me")
    public ApiResponse<?> adminMe(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(authService.getAdminMe(currentUser));
    }

    @PostMapping("/api/mobile/auth/login")
    public ApiResponse<LoginResponse> evaluatorLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.evaluatorLogin(request.username(), request.password()));
    }

    @GetMapping("/api/mobile/me")
    public ApiResponse<?> evaluatorMe(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(authService.getEvaluatorMe(currentUser));
    }
}
