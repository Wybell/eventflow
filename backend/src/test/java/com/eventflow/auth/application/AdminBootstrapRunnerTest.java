package com.eventflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eventflow.auth.domain.UserStatus;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRemainDisabledWithoutExplicitCredentials() {
        runner("", "", "").run(null);

        verifyNoInteractions(userAccountMapper, userRoleMapper, passwordEncoder);
    }

    @Test
    void shouldCreateTheFirstAdministrator() {
        when(passwordEncoder.encode("EventFlow2026!")).thenReturn("hashed-password");
        doAnswer(invocation -> {
                    UserAccount user = invocation.getArgument(0);
                    user.setId(42L);
                    return 1;
                })
                .when(userAccountMapper)
                .insert(any(UserAccount.class));
        when(userRoleMapper.findRoleCodesByUserId(42L)).thenReturn(List.of());
        when(userRoleMapper.findRoleIdByCode("ADMIN")).thenReturn(3L);

        runner("admin", "EventFlow2026!", "System Admin").run(null);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("System Admin");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        verify(userRoleMapper).insertUserRole(42L, 3L);
    }

    @Test
    void shouldNotResetAnExistingAdministratorPassword() {
        UserAccount existing = new UserAccount();
        existing.setId(42L);
        existing.setStatus(UserStatus.ACTIVE);
        when(userAccountMapper.findByUsername("admin")).thenReturn(existing);
        when(userRoleMapper.findRoleCodesByUserId(42L)).thenReturn(List.of("ADMIN"));

        runner("admin", "EventFlow2026!", "System Admin").run(null);

        verify(userAccountMapper, never()).insert(any(UserAccount.class));
        verifyNoInteractions(passwordEncoder);
        verify(userRoleMapper, never()).insertUserRole(42L, 3L);
    }

    private AdminBootstrapRunner runner(String username, String password, String displayName) {
        return new AdminBootstrapRunner(
                userAccountMapper, userRoleMapper, passwordEncoder, username, password, displayName);
    }
}
