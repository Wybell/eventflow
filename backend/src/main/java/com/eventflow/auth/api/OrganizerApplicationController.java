package com.eventflow.auth.api;

import com.eventflow.auth.api.dto.OrganizerApplicationResponse;
import com.eventflow.auth.application.OrganizerApplicationService;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizer-applications")
public class OrganizerApplicationController {
    private final OrganizerApplicationService organizerApplicationService;

    public OrganizerApplicationController(OrganizerApplicationService organizerApplicationService) {
        this.organizerApplicationService = organizerApplicationService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<OrganizerApplicationResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(organizerApplicationService.listMine(principal));
    }
}
