package com.eventflow.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 64) String username, @NotBlank @Size(min = 8, max = 72) String password) {}
