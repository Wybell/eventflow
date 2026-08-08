package com.eventflow.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
