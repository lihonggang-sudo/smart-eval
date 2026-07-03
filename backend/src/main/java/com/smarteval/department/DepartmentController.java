package com.smarteval.department;

import com.smarteval.common.ApiResponse;
import com.smarteval.common.PageResponse;
import com.smarteval.department.dto.DepartmentListItem;
import com.smarteval.department.dto.DepartmentRequest;
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
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/api/admin/departments")
    public ApiResponse<PageResponse<DepartmentListItem>> list(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "") String name
    ) {
        return ApiResponse.ok(departmentService.list(currentUser, page, pageSize, name));
    }

    @PostMapping("/api/admin/departments")
    public ApiResponse<DepartmentListItem> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.ok(departmentService.create(currentUser, request));
    }

    @PutMapping("/api/admin/departments/{id}")
    public ApiResponse<DepartmentListItem> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.ok(departmentService.update(currentUser, id, request));
    }

    @DeleteMapping("/api/admin/departments/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long id
    ) {
        departmentService.delete(currentUser, id);
        return ApiResponse.ok(null);
    }
}
