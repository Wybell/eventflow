package com.eventflow.registration.api;

import com.eventflow.registration.application.RegistrationService;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v1/registrations")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ApiResponse<IdResponse> register(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody RegistrationRequest request) {
        return ApiResponse.success(new IdResponse(registrationService.register(principal, request.toCommand())));
    }

    @GetMapping("/mine")
    public ApiResponse<List<RegistrationResponse>> listMine(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(registrationService.listMine(principal).stream()
                .map(RegistrationResponse::from)
                .toList());
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal AuthenticatedPrincipal principal, @PathVariable Long id) {
        registrationService.cancel(principal, id);
        return ApiResponse.success(null);
    }

    public record RegistrationRequest(@NotNull @Positive Long activityId, @NotNull @Positive Long sessionId) {
        private RegistrationService.RegistrationCommand toCommand() {
            return new RegistrationService.RegistrationCommand(activityId, sessionId);
        }
    }

    public record IdResponse(Long id) {}

    public record RegistrationResponse(
            Long id,
            Long activityId,
            Long sessionId,
            String status,
            String activityTitle,
            String venueName,
            String sessionTitle,
            LocalDateTime sessionStartTime,
            LocalDateTime sessionEndTime,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        private static RegistrationResponse from(RegistrationService.RegistrationView view) {
            return new RegistrationResponse(
                    view.id(),
                    view.activityId(),
                    view.sessionId(),
                    view.status().name(),
                    view.activityTitle(),
                    view.venueName(),
                    view.sessionTitle(),
                    view.sessionStartTime(),
                    view.sessionEndTime(),
                    view.createTime(),
                    view.updateTime());
        }
    }
}
