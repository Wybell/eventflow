package com.eventflow.activity.api;

import com.eventflow.activity.application.ActivityService;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @GetMapping("/public")
    public ApiResponse<List<ActivityResponse>> listPublished() {
        return ApiResponse.success(activityService.listPublished().stream()
                .map(ActivityResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityResponse> get(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        return ApiResponse.success(ActivityResponse.from(activityService.get(principal, id)));
    }

    @PostMapping("/{id}/submit-review")
    public ApiResponse<Void> submitForReview(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        activityService.submitForReview(principal, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        activityService.publish(principal, id);
        return ApiResponse.success(null);
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 500) String summary,
            @Size(max = 500) String coverUrl,
            @Size(max = 120) String venueName,
            @NotBlank @Size(max = 120) String organizerName,
            @NotBlank @Size(max = 64) String contactName,
            @Pattern(regexp = "^$|^[0-9+() -]{6,20}$") String contactMobile,
            @Email @Size(max = 255) String contactEmail,
            @NotNull LocalDateTime registrationStartTime,
            @NotNull LocalDateTime registrationEndTime) {

        private ActivityService.ActivityCommand toCommand() {
            return new ActivityService.ActivityCommand(
                    title,
                    summary,
                    coverUrl,
                    venueName,
                    organizerName,
                    contactName,
                    contactMobile,
                    contactEmail,
                    registrationStartTime,
                    registrationEndTime);
        }
    }

    public record IdResponse(Long id) {}

    public record ActivityResponse(
            Long id,
            Long createUserId,
            String title,
            String summary,
            String coverUrl,
            String venueName,
            String organizerName,
            String contactName,
            String contactMobile,
            String contactEmail,
            String status,
            String reviewNote,
            LocalDateTime reviewTime,
            LocalDateTime publishedTime,
            LocalDateTime registrationStartTime,
            LocalDateTime registrationEndTime) {

        public static ActivityResponse from(Activity activity) {
            return new ActivityResponse(
                    activity.getId(),
                    activity.getCreateUserId(),
                    activity.getTitle(),
                    activity.getSummary(),
                    activity.getCoverUrl(),
                    activity.getVenueName(),
                    activity.getOrganizerName(),
                    activity.getContactName(),
                    activity.getContactMobile(),
                    activity.getContactEmail(),
                    activity.getStatus().name(),
                    activity.getReviewNote(),
                    activity.getReviewTime(),
                    activity.getPublishedTime(),
                    activity.getRegistrationStartTime(),
                    activity.getRegistrationEndTime());
        }
    }
}
