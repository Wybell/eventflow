package com.eventflow.activity.api;

import com.eventflow.activity.application.ActivityService;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ApiResponse<IdResponse> create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @Valid @RequestBody ActivityRequest request) {
        return ApiResponse.success(new IdResponse(activityService.create(principal, request.toCommand())));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        activityService.update(principal, id, request.toCommand());
        return ApiResponse.success(null);
    }

    @GetMapping("/mine")
    public ApiResponse<List<ActivityResponse>> listMine(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(activityService.listMine(principal).stream()
                .map(ActivityResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityResponse> get(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        return ApiResponse.success(ActivityResponse.from(activityService.get(principal, id)));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        activityService.publish(principal, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offline(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        activityService.offline(principal, id);
        return ApiResponse.success(null);
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 500) String summary,
            @Size(max = 500) String coverUrl,
            @Size(max = 120) String venueName,
            @NotNull LocalDateTime registrationStartTime,
            @NotNull LocalDateTime registrationEndTime) {

        private ActivityService.ActivityCommand toCommand() {
            return new ActivityService.ActivityCommand(
                    title, summary, coverUrl, venueName, registrationStartTime, registrationEndTime);
        }
    }

    public record IdResponse(Long id) {}

    public record ActivityResponse(
            Long id,
            Long organizationId,
            String title,
            String summary,
            String coverUrl,
            String venueName,
            String status,
            LocalDateTime registrationStartTime,
            LocalDateTime registrationEndTime) {

        private static ActivityResponse from(Activity activity) {
            return new ActivityResponse(
                    activity.getId(),
                    activity.getOrganizationId(),
                    activity.getTitle(),
                    activity.getSummary(),
                    activity.getCoverUrl(),
                    activity.getVenueName(),
                    activity.getStatus().name(),
                    activity.getRegistrationStartTime(),
                    activity.getRegistrationEndTime());
        }
    }
}
