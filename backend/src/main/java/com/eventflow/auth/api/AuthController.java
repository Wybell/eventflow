package com.eventflow.auth.api;

import com.eventflow.auth.api.dto.AuthTokenResponse;
import com.eventflow.auth.api.dto.AvatarResponse;
import com.eventflow.auth.api.dto.ChangePasswordRequest;
import com.eventflow.auth.api.dto.CurrentUserResponse;
import com.eventflow.auth.api.dto.LoginRequest;
import com.eventflow.auth.api.dto.RefreshTokenRequest;
import com.eventflow.auth.api.dto.RegisterRequest;
import com.eventflow.auth.api.dto.RegistrationResponse;
import com.eventflow.auth.api.dto.UpdateProfileRequest;
import com.eventflow.auth.application.AccountRegistrationService;
import com.eventflow.auth.application.AuthService;
import com.eventflow.auth.application.UserProfileService;
import com.eventflow.shared.api.ApiResponse;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountRegistrationService accountRegistrationService;
    private final UserProfileService userProfileService;

    public AuthController(
            AuthService authService,
            AccountRegistrationService accountRegistrationService,
            UserProfileService userProfileService) {
        this.authService = authService;
        this.accountRegistrationService = accountRegistrationService;
        this.userProfileService = userProfileService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(accountRegistrationService.register(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.success(authService.currentUser(principal));
    }

    @PutMapping("/me")
    public ApiResponse<CurrentUserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userProfileService.update(principal, request));
    }

    @PostMapping("/me/avatar")
    public ApiResponse<AvatarResponse> updateAvatar(
            @AuthenticationPrincipal AuthenticatedPrincipal principal, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(new AvatarResponse(userProfileService.updateAvatar(principal, file)));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(principal, request);
        return ApiResponse.success(null);
    }
}
