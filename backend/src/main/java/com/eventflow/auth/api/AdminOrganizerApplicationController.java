package com.eventflow.auth.api;

import com.eventflow.auth.api.dto.OrganizerApplicationResponse;
import com.eventflow.auth.application.OrganizerApplicationService;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/organizer-applications")
public class AdminOrganizerApplicationController {
    private final OrganizerApplicationService organizerApplicationService;

    public AdminOrganizerApplicationController(OrganizerApplicationService organizerApplicationService) {
        this.organizerApplicationService = organizerApplicationService;
    }

    @GetMapping
    public ApiResponse<List<OrganizerApplicationResponse>> listPending(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(organizerApplicationService.listPending(principal));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        organizerApplicationService.approve(principal, id, request.reviewNote());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        organizerApplicationService.reject(principal, id, request.reviewNote());
        return ApiResponse.success(null);
    }

    public record ReviewRequest(@Size(max = 500) String reviewNote) {}
}
