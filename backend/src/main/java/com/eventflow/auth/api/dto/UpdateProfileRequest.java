package com.eventflow.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 64) String displayName,
        @Pattern(regexp = "^$|^[0-9+() -]{6,20}$") String mobile,
        @Email @Size(max = 255) String email) {}
