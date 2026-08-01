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
import com.eventflow.auth.domain.RegistrationType;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplication;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplicationMapper;
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
    private OrganizerApplicationMapper organizerApplicationMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateAnActiveParticipantWithTheUserRole() {
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

        RegistrationResponse response = service.register(participantRequest());

        assertThat(response.userId()).isEqualTo(21L);
        assertThat(response.registrationType()).isEqualTo("PARTICIPANT");
        assertThat(response.organizerApplicationStatus()).isNull();
        verify(userRoleMapper).insertUserRole(21L, 3L);
        verify(organizerApplicationMapper, never()).insert(any(OrganizerApplication.class));
    }

    @Test
    void shouldCreatePendingOrganizerApplicationWithoutGrantingOrganizerRole() {
        when(passwordEncoder.encode("EventFlow2026!")).thenReturn("hashed-password");
        when(userRoleMapper.findRoleIdByCode("USER")).thenReturn(3L);
        doAnswer(invocation -> {
                    UserAccount user = invocation.getArgument(0);
                    user.setId(22L);
                    return 1;
                })
                .when(userAccountMapper)
                .insert(any(UserAccount.class));
        AccountRegistrationService service = service();

        RegistrationResponse response = service.register(organizerRequest());

        ArgumentCaptor<OrganizerApplication> applicationCaptor = ArgumentCaptor.forClass(OrganizerApplication.class);
        verify(organizerApplicationMapper).insert(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getUserId()).isEqualTo(22L);
        assertThat(applicationCaptor.getValue().getStatus().name()).isEqualTo("PENDING");
        assertThat(response.organizerApplicationStatus()).isEqualTo("PENDING");
        verify(userRoleMapper).insertUserRole(22L, 3L);
    }

    @Test
    void shouldRejectOrganizerRegistrationWithoutOrganizationDetails() {
        AccountRegistrationService service = service();
        RegisterRequest request = new RegisterRequest(
                RegistrationType.ORGANIZER, "organizer", "EventFlow2026!", "Lin", null, null, null, null, null);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(userAccountMapper, never()).insert(any(UserAccount.class));
    }

    private AccountRegistrationService service() {
        return new AccountRegistrationService(
                userAccountMapper, userRoleMapper, organizerApplicationMapper, passwordEncoder);
    }

    private RegisterRequest participantRequest() {
        return new RegisterRequest(
                RegistrationType.PARTICIPANT,
                "participant",
                "EventFlow2026!",
                "Wang",
                "13800138000",
                "wang@example.com",
                null,
                null,
                null);
    }

    private RegisterRequest organizerRequest() {
        return new RegisterRequest(
                RegistrationType.ORGANIZER,
                "organizer",
                "EventFlow2026!",
                "Lin",
                "13800138001",
                "lin@example.com",
                "EventFlow Community",
                "A local technology community.",
                "Lin");
    }
}
