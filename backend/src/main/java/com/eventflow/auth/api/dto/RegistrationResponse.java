package com.eventflow.auth.api.dto;

public record RegistrationResponse(Long userId, String registrationType, String organizerApplicationStatus) {}
