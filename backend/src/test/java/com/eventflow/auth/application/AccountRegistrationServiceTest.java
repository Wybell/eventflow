package com.eventflow.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventflow.auth.api.dto.RegisterRequest;
import com.eventflow.auth.api.dto.RegistrationResponse;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationServiceTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateAnActiveUserWithTheUserRole() {
        when(passwordEncoder.encode("EventFlow2026!")).thenReturn("hashed-password");
        when(userRoleMapper.findRoleIdByCode("USER")).thenReturn(3L);
        doAnswer(invocation -> {
                    UserAccount user = invocation.getArgument(0);
                    user.setId(21L);
                    return 1;
                })
                .when(userAccountMapper)
                .insert(any(UserAccount.class));
        AccountRegistrationService service = service();

        RegistrationResponse response = service.register(request());

        assertThat(response.userId()).isEqualTo(21L);
        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getOrganizationId()).isNull();
        verify(userRoleMapper).insertUserRole(21L, 3L);
    }

    @Test
    void shouldRejectAnExistingMobileNumberWithConflict() {
        when(userAccountMapper.findByMobile("13800138000")).thenReturn(new UserAccount());
        AccountRegistrationService service = service();

        assertThatThrownBy(() -> service.register(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.CONFLICT);

        verify(userAccountMapper, never()).insert(any(UserAccount.class));
    }

    private AccountRegistrationService service() {
        return new AccountRegistrationService(userAccountMapper, userRoleMapper, passwordEncoder);
    }

    private RegisterRequest request() {
        return new RegisterRequest("participant", "EventFlow2026!", "Wang", "13800138000", "wang@example.com");
    }
}
