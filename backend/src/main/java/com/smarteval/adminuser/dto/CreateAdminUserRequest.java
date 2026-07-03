package com.smarteval.adminuser.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminUserRequest(@NotBlank String username, @NotBlank String password,
                                     @NotBlank String displayName) {
}
