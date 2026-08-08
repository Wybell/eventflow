package com.eventflow.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventflow.activity.domain.ActivitySessionStatus;
import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.registration.domain.RegistrationStatus;
import com.eventflow.registration.infrastructure.persistence.ActivityRegistration;
import com.eventflow.registration.infrastructure.persistence.ActivityRegistrationMapper;
import com.eventflow.registration.infrastructure.persistence.OrganizerRegistrationRow;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 12, 0);

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivitySessionMapper activitySessionMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Test
    void shouldConfirmRegistrationAndDecrementAvailableQuota() {
        givenOpenActivityAndSession();
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(activitySessionMapper.confirmQuota(11L, 21L)).thenReturn(1);
        RegistrationService service = service();

        service.register(user(7L), command());

        verify(activitySessionMapper).confirmQuota(11L, 21L);
        ArgumentCaptor<ActivityRegistration> captor = ArgumentCaptor.forClass(ActivityRegistration.class);
        verify(registrationMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    void shouldRejectRegistrationForNonPublishedActivity() {
        when(activityMapper.selectById(11L))
                .thenReturn(activity(ActivityStatus.APPROVED, NOW.minusHours(1), NOW.plusHours(1)));
        RegistrationService service = service();

        assertConflict(() -> service.register(user(7L), command()));

        verify(activitySessionMapper, never()).confirmQuota(any(), any());
    }

    @Test
    void shouldRejectRegistrationBeforeOpening() {
        when(activityMapper.selectById(11L))
                .thenReturn(activity(ActivityStatus.PUBLISHED, NOW.plusMinutes(1), NOW.plusHours(2)));
        when(activitySessionMapper.selectById(21L)).thenReturn(session(NOW.plusDays(1), 5));
        RegistrationService service = service();

        assertConflict(() -> service.register(user(7L), command()));

        verify(activitySessionMapper, never()).confirmQuota(any(), any());
    }

    @Test
    void shouldEvaluateRegistrationWindowInShanghaiTime() {
        when(activityMapper.selectById(11L))
                .thenReturn(activity(
                        ActivityStatus.PUBLISHED,
                        LocalDateTime.of(2026, 8, 8, 17, 0),
                        LocalDateTime.of(2026, 8, 8, 17, 5)));
        when(activitySessionMapper.selectById(21L)).thenReturn(session(LocalDateTime.of(2026, 8, 8, 20, 0), 5));
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(activitySessionMapper.confirmQuota(11L, 21L)).thenReturn(1);

        RegistrationService service =
                service(Clock.fixed(Instant.parse("2026-08-08T09:02:00Z"), ZoneId.of("Asia/Shanghai")));

        service.register(user(7L), command());

        verify(registrationMapper).insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldRejectRegistrationAfterClosing() {
        when(activityMapper.selectById(11L)).thenReturn(activity(ActivityStatus.PUBLISHED, NOW.minusHours(2), NOW));
        when(activitySessionMapper.selectById(21L)).thenReturn(session(NOW.plusDays(1), 5));
        RegistrationService service = service();

        assertConflict(() -> service.register(user(7L), command()));

        verify(activitySessionMapper, never()).confirmQuota(any(), any());
    }

    @Test
    void shouldRejectDuplicateActiveRegistration() {
        givenOpenActivityAndSession();
        when(registrationMapper.selectOne(any())).thenReturn(registration(31L, 7L, RegistrationStatus.CONFIRMED));
        RegistrationService service = service();

        assertConflict(() -> service.register(user(7L), command()));

        verify(activitySessionMapper, never()).confirmQuota(any(), any());
    }

    @Test
    void shouldRejectFullSessionWithoutCreatingRegistration() {
        givenOpenActivityAndSession();
        when(registrationMapper.selectOne(any())).thenReturn(null);
        when(activitySessionMapper.confirmQuota(11L, 21L)).thenReturn(0);
        RegistrationService service = service();

        assertConflict(() -> service.register(user(7L), command()));

        verify(registrationMapper, never()).insert(any(ActivityRegistration.class));
    }

    @Test
    void shouldReturnQuotaWhenCancelling() {
        ActivityRegistration registration = registration(31L, 7L, RegistrationStatus.CONFIRMED);
        when(registrationMapper.selectById(31L)).thenReturn(registration);
        when(activitySessionMapper.releaseConfirmedQuota(11L, 21L)).thenReturn(1);
        when(registrationMapper.cancel(31L, 7L)).thenReturn(1);
        RegistrationService service = service();

        service.cancel(user(7L), 31L);

        verify(activitySessionMapper).releaseConfirmedQuota(11L, 21L);
        verify(registrationMapper).cancel(31L, 7L);
    }

    @Test
    void shouldRejectCancellingAnotherUsersRegistration() {
        when(registrationMapper.selectById(31L)).thenReturn(registration(31L, 9L, RegistrationStatus.CONFIRMED));
        RegistrationService service = service();

        assertThatThrownBy(() -> service.cancel(user(7L), 31L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(activitySessionMapper, never()).releaseConfirmedQuota(any(), any());
    }

    @Test
    void shouldListOrganizerRegistrationsWithMaskedContactDetails() {
        Activity activity = activity(ActivityStatus.PUBLISHED, NOW.minusHours(1), NOW.plusHours(1));
        activity.setCreateUserId(7L);
        when(activityMapper.selectById(11L)).thenReturn(activity);
        OrganizerRegistrationRow row = new OrganizerRegistrationRow();
        row.setId(31L);
        row.setActivityId(11L);
        row.setSessionId(21L);
        row.setStatus("CONFIRMED");
        row.setDisplayName("张三");
        row.setUsername("zhangsan");
        row.setMobile("13812341234");
        row.setEmail("zhangsan@example.com");
        row.setSessionTitle("晚场");
        when(registrationMapper.countForOrganizer(11L, null, RegistrationStatus.CONFIRMED, null))
                .thenReturn(1L);
        when(registrationMapper.countByActivityAndStatus(11L, RegistrationStatus.CONFIRMED))
                .thenReturn(1L);
        when(registrationMapper.countByActivityAndStatus(11L, RegistrationStatus.CANCELLED))
                .thenReturn(0L);
        when(registrationMapper.findForOrganizer(11L, null, RegistrationStatus.CONFIRMED, null, 0L, 20))
                .thenReturn(List.of(row));

        RegistrationService.OrganizerRegistrationPage result =
                service().listForOrganizer(user(7L), 11L, null, "CONFIRMED", null, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().get(0).mobile()).isEqualTo("138****1234");
        assertThat(result.items().get(0).email()).isEqualTo("z***@example.com");
    }

    @Test
    void shouldRejectOrganizerRegistrationListForAnotherUser() {
        Activity activity = activity(ActivityStatus.PUBLISHED, NOW.minusHours(1), NOW.plusHours(1));
        activity.setCreateUserId(9L);
        when(activityMapper.selectById(11L)).thenReturn(activity);

        assertThatThrownBy(() -> service().listForOrganizer(user(7L), 11L, null, null, null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(registrationMapper, never()).findForOrganizer(any(), any(), any(), any(), anyLong(), anyInt());
    }

    private void givenOpenActivityAndSession() {
        when(activityMapper.selectById(11L))
                .thenReturn(activity(ActivityStatus.PUBLISHED, NOW.minusHours(1), NOW.plusHours(1)));
        when(activitySessionMapper.selectById(21L)).thenReturn(session(NOW.plusDays(1), 5));
    }

    private RegistrationService service() {
        return service(Clock.fixed(Instant.parse("2026-08-08T12:00:00Z"), ZoneOffset.UTC));
    }

    private RegistrationService service(Clock clock) {
        return new RegistrationService(activityMapper, activitySessionMapper, registrationMapper, clock);
    }

    private Activity activity(ActivityStatus status, LocalDateTime registrationStart, LocalDateTime registrationEnd) {
        Activity activity = new Activity();
        activity.setId(11L);
        activity.setStatus(status);
        activity.setRegistrationStartTime(registrationStart);
        activity.setRegistrationEndTime(registrationEnd);
        return activity;
    }

    private ActivitySession session(LocalDateTime startTime, int availableQuota) {
        ActivitySession session = new ActivitySession();
        session.setId(21L);
        session.setActivityId(11L);
        session.setStartTime(startTime);
        session.setAvailableQuota(availableQuota);
        session.setStatus(ActivitySessionStatus.ACTIVE);
        return session;
    }

    private ActivityRegistration registration(Long id, Long userId, RegistrationStatus status) {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(id);
        registration.setActivityId(11L);
        registration.setSessionId(21L);
        registration.setUserId(userId);
        registration.setStatus(status);
        return registration;
    }

    private RegistrationService.RegistrationCommand command() {
        return new RegistrationService.RegistrationCommand(11L, 21L);
    }

    private AuthenticatedPrincipal user(Long userId) {
        return new AuthenticatedPrincipal(userId, null, Set.of("USER"));
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }
}
