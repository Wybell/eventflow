package com.eventflow.activity.api;

import com.eventflow.activity.application.ActivitySessionService;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities/{activityId}/sessions")
public class ActivitySessionController {
    private final ActivitySessionService activitySessionService;

    public ActivitySessionController(ActivitySessionService activitySessionService) {
        this.activitySessionService = activitySessionService;
    }

    @PostMapping
    public ApiResponse<ActivityController.IdResponse> create(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long activityId,
            @Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(new ActivityController.IdResponse(
                activitySessionService.create(principal, activityId, request.toCommand())));
    }

    @GetMapping
    public ApiResponse<List<SessionResponse>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long activityId) {
        return ApiResponse.success(activitySessionService.list(principal, activityId).stream()
                .map(SessionResponse::from)
                .toList());
    }

    @GetMapping("/public")
    public ApiResponse<List<SessionResponse>> listPublished(@PathVariable Long activityId) {
        return ApiResponse.success(activitySessionService.listPublished(activityId).stream()
                .map(SessionResponse::from)
                .toList());
    }

    public record CreateRequest(
            @NotBlank @Size(max = 120) String title,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            @Positive int totalQuota) {

        private ActivitySessionService.SessionCommand toCommand() {
            return new ActivitySessionService.SessionCommand(title, startTime, endTime, totalQuota);
        }
    }

    public record SessionResponse(
            Long id,
            Long activityId,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer totalQuota,
            Integer availableQuota,
            Integer reservedQuota,
            Integer confirmedQuota,
            String status) {

        private static SessionResponse from(ActivitySession session) {
            return new SessionResponse(
                    session.getId(),
                    session.getActivityId(),
                    session.getTitle(),
                    session.getStartTime(),
                    session.getEndTime(),
                    session.getTotalQuota(),
                    session.getAvailableQuota(),
                    session.getReservedQuota(),
                    session.getConfirmedQuota(),
                    session.getStatus().name());
        }
    }
}
