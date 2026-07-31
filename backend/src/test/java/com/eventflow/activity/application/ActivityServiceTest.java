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
class ActivityServiceTest {

    @Mock
    private ActivityMapper activityMapper;

    @Test
    void shouldRejectPublishingAnActivityOutsideTheOrganizerOrganization() {
        Activity activity = activity(18L, 2L, ActivityStatus.DRAFT);
        when(activityMapper.selectById(18L)).thenReturn(activity);
        ActivityService service = new ActivityService(activityMapper);

        assertThatThrownBy(() -> service.publish(organizer(1L), 18L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void shouldRejectAnInvalidRegistrationWindow() {
        ActivityService service = new ActivityService(activityMapper);
        LocalDateTime time = LocalDateTime.of(2026, 8, 1, 9, 0);

        assertThatThrownBy(() -> service.create(organizer(1L), command(time, time)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(activityMapper, never()).insert(any(Activity.class));
    }

    @Test
    void shouldPublishDraftActivityOwnedByOrganizer() {
        Activity activity = activity(18L, 1L, ActivityStatus.DRAFT);
        when(activityMapper.selectById(18L)).thenReturn(activity);
        ActivityService service = new ActivityService(activityMapper);

        service.publish(organizer(1L), 18L);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
    }

    private Activity activity(Long id, Long organizationId, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setOrganizationId(organizationId);
        activity.setStatus(status);
        return activity;
    }

    private AuthenticatedPrincipal organizer(Long organizationId) {
        return new AuthenticatedPrincipal(7L, organizationId, Set.of("ORGANIZER"));
    }

    private ActivityService.ActivityCommand command(LocalDateTime start, LocalDateTime end) {
        return new ActivityService.ActivityCommand("Spring Camp", null, null, null, start, end);
    }
}
