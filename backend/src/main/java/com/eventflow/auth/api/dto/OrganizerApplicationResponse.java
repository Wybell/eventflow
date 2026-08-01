package com.eventflow.auth.api.dto;

import java.time.LocalDateTime;

public record OrganizerApplicationResponse(
        Long id,
        String organizationName,
        String status,
        String reviewNote,
        LocalDateTime createTime,
        LocalDateTime reviewTime) {}
