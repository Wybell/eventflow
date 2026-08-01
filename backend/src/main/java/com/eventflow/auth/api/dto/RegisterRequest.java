package com.eventflow.auth.api.dto;

import com.eventflow.auth.domain.RegistrationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull RegistrationType registrationType,
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 64) String displayName,
        @Pattern(regexp = "^$|^[0-9+() -]{6,20}$") String mobile,
        @Email @Size(max = 255) String email,
        @Size(max = 120) String organizationName,
        @Size(max = 500) String organizationDescription,
        @Size(max = 64) String contactName) {}
