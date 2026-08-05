package com.eventflow.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivitySessionServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivitySessionMapper activitySessionMapper;

    @Test
    void shouldInitializeAllQuotaAsAvailableWhenCreatingASession() {
        when(activityMapper.selectById(12L)).thenReturn(activity(12L, 7L, ActivityStatus.DRAFT));
        ActivitySessionService service = new ActivitySessionService(activityMapper, activitySessionMapper);
        LocalDateTime start = LocalDateTime.of(2026, 8, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 3, 10, 0);

        service.create(user(7L), 12L, new ActivitySessionService.SessionCommand("Java 17", start, end, 80));

        ArgumentCaptor<ActivitySession> sessionCaptor = ArgumentCaptor.forClass(ActivitySession.class);
        verify(activitySessionMapper).insert(sessionCaptor.capture());
        ActivitySession session = sessionCaptor.getValue();
        assertThat(session.getAvailableQuota()).isEqualTo(80);
        assertThat(session.getReservedQuota()).isZero();
        assertThat(session.getConfirmedQuota()).isZero();
    }

    @Test
    void shouldRejectSessionWithInvalidTimeRange() {
        when(activityMapper.selectById(12L)).thenReturn(activity(12L, 7L, ActivityStatus.DRAFT));
        ActivitySessionService service = new ActivitySessionService(activityMapper, activitySessionMapper);
        LocalDateTime time = LocalDateTime.of(2026, 8, 3, 9, 0);

        assertThatThrownBy(() -> service.create(
                        user(7L), 12L, new ActivitySessionService.SessionCommand("Java 17", time, time, 80)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(activitySessionMapper, never()).insert(any(ActivitySession.class));
    }

    private Activity activity(Long id, Long creatorUserId, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setCreateUserId(creatorUserId);
        activity.setStatus(status);
        return activity;
    }

    private AuthenticatedPrincipal user(Long userId) {
        return new AuthenticatedPrincipal(userId, null, Set.of("USER"));
    }
}
