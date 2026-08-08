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
import com.eventflow.activity.infrastructure.persistence.ActivityReviewRecord;
import com.eventflow.activity.infrastructure.persistence.ActivityReviewRecordMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @Mock
    private ActivitySessionMapper activitySessionMapper;

    @Mock
    private ActivityReviewRecordMapper activityReviewRecordMapper;

    @Test
    void shouldAllowAnyAuthenticatedUserToCreateDraft() {
        ActivityService service = service();
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 9, 0);

        service.create(user(7L), command(start, start.plusHours(1)));

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).insert(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getCreateUserId()).isEqualTo(7L);
        assertThat(activityCaptor.getValue().getOrganizationId()).isNull();
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(ActivityStatus.DRAFT);
    }

    @Test
    void shouldRejectSubmittingAnotherUsersActivity() {
        when(activityMapper.selectById(18L)).thenReturn(activity(18L, 9L, ActivityStatus.DRAFT));
        ActivityService service = service();

        assertThatThrownBy(() -> service.submitForReview(user(7L), 18L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void shouldRequireAtLeastOneSessionBeforeSubmittingForReview() {
        when(activityMapper.selectById(18L)).thenReturn(activity(18L, 7L, ActivityStatus.DRAFT));
        when(activitySessionMapper.selectCount(any())).thenReturn(0L);
        ActivityService service = service();

        assertThatThrownBy(() -> service.submitForReview(user(7L), 18L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void shouldMarkPendingActivityAsApprovedByAdministrator() {
        Activity activity = activity(18L, 7L, ActivityStatus.PENDING_REVIEW);
        when(activityMapper.selectById(18L)).thenReturn(activity);
        ActivityService service = service();

        service.approve(admin(1L), 18L, "符合发布要求");

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(ActivityStatus.APPROVED);
        assertThat(activityCaptor.getValue().getReviewUserId()).isEqualTo(1L);
        verify(activityReviewRecordMapper).insert(any(ActivityReviewRecord.class));
    }

    @Test
    void shouldAllowCreatorToPublishApprovedActivity() {
        Activity activity = activity(18L, 7L, ActivityStatus.APPROVED);
        when(activityMapper.selectById(18L)).thenReturn(activity);
        ActivityService service = service();

        service.publish(user(7L), 18L);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(ActivityStatus.PUBLISHED);
        assertThat(activityCaptor.getValue().getPublishedTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 0, 0));
    }

    @Test
    void shouldRejectPublishingAnotherUsersApprovedActivity() {
        when(activityMapper.selectById(18L)).thenReturn(activity(18L, 9L, ActivityStatus.APPROVED));
        ActivityService service = service();

        assertThatThrownBy(() -> service.publish(user(7L), 18L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(activityMapper, never()).updateById(any(Activity.class));
    }

    @Test
    void shouldRejectAnInvalidRegistrationWindow() {
        ActivityService service = service();
        LocalDateTime time = LocalDateTime.of(2026, 8, 1, 9, 0);

        assertThatThrownBy(() -> service.create(user(7L), command(time, time)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);

        verify(activityMapper, never()).insert(any(Activity.class));
    }

    private ActivityService service() {
        return new ActivityService(
                activityMapper,
                activitySessionMapper,
                activityReviewRecordMapper,
                Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC));
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

    private AuthenticatedPrincipal admin(Long userId) {
        return new AuthenticatedPrincipal(userId, null, Set.of("ADMIN"));
    }

    private ActivityService.ActivityCommand command(LocalDateTime start, LocalDateTime end) {
        return new ActivityService.ActivityCommand(
                "Spring Camp",
                null,
                null,
                null,
                "EventFlow Community",
                "Lin",
                "13800138000",
                "lin@example.com",
                start,
                end);
    }
}
