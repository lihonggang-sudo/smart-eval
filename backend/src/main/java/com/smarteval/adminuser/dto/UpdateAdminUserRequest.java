package com.smarteval.adminuser.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAdminUserRequest(@NotBlank String displayName) {
}
