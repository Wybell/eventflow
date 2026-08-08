package com.eventflow.registration.api;

import com.eventflow.registration.application.RegistrationService;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/activities/{activityId}/registrations")
public class ActivityRegistrationController {
    private final RegistrationService registrationService;

    public ActivityRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public ApiResponse<RegistrationPageResponse> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable Long activityId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        RegistrationService.OrganizerRegistrationPage result =
                registrationService.listForOrganizer(principal, activityId, sessionId, status, keyword, page, size);
        return ApiResponse.success(RegistrationPageResponse.from(result));
    }

    public record RegistrationPageResponse(
            long total, long confirmedCount, long cancelledCount, int page, int size, List<ParticipantResponse> items) {

        private static RegistrationPageResponse from(RegistrationService.OrganizerRegistrationPage page) {
            return new RegistrationPageResponse(
                    page.total(),
                    page.confirmedCount(),
                    page.cancelledCount(),
                    page.page(),
                    page.size(),
                    page.items().stream().map(ParticipantResponse::from).toList());
        }
    }

    public record ParticipantResponse(
            Long id,
            Long activityId,
            Long sessionId,
            String status,
            String displayName,
            String username,
            String mobile,
            String email,
            String sessionTitle,
            LocalDateTime registrationTime,
            LocalDateTime updateTime) {

        private static ParticipantResponse from(RegistrationService.OrganizerRegistrationView view) {
            return new ParticipantResponse(
                    view.id(),
                    view.activityId(),
                    view.sessionId(),
                    view.status(),
                    view.displayName(),
                    view.username(),
                    view.mobile(),
                    view.email(),
                    view.sessionTitle(),
                    view.registrationTime(),
                    view.updateTime());
        }
    }
}
