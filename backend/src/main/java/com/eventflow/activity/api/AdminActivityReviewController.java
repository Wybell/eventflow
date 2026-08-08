package com.eventflow.activity.api;

import com.eventflow.activity.application.ActivityService;
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
@RequestMapping("/api/v1/admin/activities")
public class AdminActivityReviewController {

    private final ActivityService activityService;

    public AdminActivityReviewController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/pending-review")
    public ApiResponse<List<ActivityController.ActivityResponse>> listPendingReview(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(activityService.listPendingReview(principal).stream()
                .map(ActivityController.ActivityResponse::from)
                .toList());
    }

    @GetMapping("/reviewed")
    public ApiResponse<List<ActivityController.ActivityResponse>> listReviewed(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(activityService.listReviewed(principal).stream()
                .map(ActivityController.ActivityResponse::from)
                .toList());
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        activityService.approve(principal, id, request.reviewNote());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        activityService.reject(principal, id, request.reviewNote());
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offline(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        activityService.offline(principal, id, request.reviewNote());
        return ApiResponse.success(null);
    }

    public record ReviewRequest(@Size(max = 500) String reviewNote) {}
}
