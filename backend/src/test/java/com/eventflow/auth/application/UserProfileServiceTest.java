package com.eventflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventflow.auth.api.dto.ChangePasswordRequest;
import com.eventflow.auth.api.dto.CurrentUserResponse;
import com.eventflow.auth.api.dto.UpdateProfileRequest;
import com.eventflow.auth.domain.UserStatus;
import com.eventflow.auth.infrastructure.persistence.RefreshTokenMapper;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @TempDir
    private Path avatarDirectory;

    @Test
    void shouldUpdateCurrentUsersBasicProfile() {
        UserAccount user = activeUser();
        when(userAccountMapper.selectById(7L)).thenReturn(user);
        when(userRoleMapper.findRoleCodesByUserId(7L)).thenReturn(List.of("USER"));

        CurrentUserResponse response =
                service().update(principal(), new UpdateProfileRequest("New Name", "13800138000", "new@example.com"));

        verify(userAccountMapper).updateById(user);
        assertThat(response.displayName()).isEqualTo("New Name");
        assertThat(response.mobile()).isEqualTo("13800138000");
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void shouldRejectAnIncorrectCurrentPassword() {
        UserAccount user = activeUser();
        when(userAccountMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service()
                        .changePassword(principal(), new ChangePasswordRequest("wrong-password", "NewPassword2026!")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(userAccountMapper, never()).updateById(any(UserAccount.class));
        verify(refreshTokenMapper, never()).revokeAllByUserId(any(), any());
    }

    @Test
    void shouldChangePasswordAndRevokeRefreshTokens() {
        UserAccount user = activeUser();
        when(userAccountMapper.selectById(7L)).thenReturn(user);
        when(passwordEncoder.matches("CurrentPassword2026!", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword2026!", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword2026!")).thenReturn("new-hash");

        service().changePassword(principal(), new ChangePasswordRequest("CurrentPassword2026!", "NewPassword2026!"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(userAccountMapper).updateById(user);
        verify(refreshTokenMapper)
                .revokeAllByUserId(
                        7L,
                        Instant.parse("2026-08-08T00:00:00Z")
                                .atOffset(ZoneOffset.UTC)
                                .toLocalDateTime());
    }

    @Test
    void shouldRejectAvatarWhenContentDoesNotMatchDeclaredType() {
        when(userAccountMapper.selectById(7L)).thenReturn(activeUser());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "not-an-image".getBytes());

        assertThatThrownBy(() -> service().updateAvatar(principal(), file))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(userAccountMapper, never()).updateById(any(UserAccount.class));
    }

    @Test
    void shouldStoreAValidAvatarWithARandomFileName() {
        UserAccount user = activeUser();
        when(userAccountMapper.selectById(7L)).thenReturn(user);
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", pngHeader);

        String avatarUrl = service().updateAvatar(principal(), file);

        assertThat(avatarUrl).matches("/api/v1/avatars/[0-9a-f-]{36}\\.png");
        assertThat(avatarDirectory.toFile().listFiles()).hasSize(1);
        verify(userAccountMapper).updateById(user);
    }

    private UserProfileService service() {
        return new UserProfileService(
                userAccountMapper,
                userRoleMapper,
                refreshTokenMapper,
                passwordEncoder,
                Clock.fixed(Instant.parse("2026-08-08T00:00:00Z"), ZoneOffset.UTC),
                avatarDirectory.toString());
    }

    private UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setId(7L);
        user.setUsername("wybell");
        user.setDisplayName("Wybell");
        user.setPasswordHash("old-hash");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(7L, null, Set.of("USER"));
    }
}
