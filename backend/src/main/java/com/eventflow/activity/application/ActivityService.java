package com.eventflow.activity.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.activity.infrastructure.persistence.ActivityReviewRecord;
import com.eventflow.activity.infrastructure.persistence.ActivityReviewRecordMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private static final String ADMIN_ROLE = "ADMIN";

    private final ActivityMapper activityMapper;
    private final ActivitySessionMapper activitySessionMapper;
    private final ActivityReviewRecordMapper activityReviewRecordMapper;
    private final Clock clock;

    public ActivityService(
            ActivityMapper activityMapper,
            ActivitySessionMapper activitySessionMapper,
            ActivityReviewRecordMapper activityReviewRecordMapper,
            Clock clock) {
        this.activityMapper = activityMapper;
        this.activitySessionMapper = activitySessionMapper;
        this.activityReviewRecordMapper = activityReviewRecordMapper;
        this.clock = clock;
    }

    @Transactional
    public Long create(AuthenticatedPrincipal principal, ActivityCommand command) {
        validateCommand(command);

        Activity activity = new Activity();
        activity.setCreateUserId(principal.userId());
        applyCommand(activity, command);
        activity.setStatus(ActivityStatus.DRAFT);
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Transactional
    public void update(AuthenticatedPrincipal principal, Long id, ActivityCommand command) {
        validateCommand(command);
        Activity activity = findRequired(id);
        requireCreator(principal, activity);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Only draft or rejected activities can be edited");
        }
        applyCommand(activity, command);
        activityMapper.updateById(activity);
    }

    @Transactional(readOnly = true)
    public List<Activity> listMine(AuthenticatedPrincipal principal) {
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getCreateUserId, principal.userId())
                .orderByDesc(Activity::getId));
    }

    @Transactional(readOnly = true)
    public List<Activity> listPublished() {
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, ActivityStatus.PUBLISHED)
                .orderByDesc(Activity::getPublishedTime)
                .orderByDesc(Activity::getId));
    }

    @Transactional(readOnly = true)
    public List<Activity> listPendingReview(AuthenticatedPrincipal principal) {
        requireAdmin(principal);
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, ActivityStatus.PENDING_REVIEW)
                .orderByAsc(Activity::getId));
    }

    @Transactional(readOnly = true)
    public List<Activity> listReviewed(AuthenticatedPrincipal principal) {
        requireAdmin(principal);
        List<Long> activityIds = activityReviewRecordMapper
                .selectList(new LambdaQueryWrapper<ActivityReviewRecord>()
                        .eq(ActivityReviewRecord::getReviewerUserId, principal.userId())
                        .orderByDesc(ActivityReviewRecord::getId))
                .stream()
                .map(ActivityReviewRecord::getActivityId)
                .distinct()
                .toList();
        if (activityIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Activity> activitiesById = activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
        return activityIds.stream()
                .map(activitiesById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public Activity get(AuthenticatedPrincipal principal, Long id) {
        Activity activity = findRequired(id);
        if (!hasRole(principal, ADMIN_ROLE) && !activity.getCreateUserId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return activity;
    }

    @Transactional
    public void submitForReview(AuthenticatedPrincipal principal, Long id) {
        Activity activity = findRequired(id);
        requireCreator(principal, activity);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT, "Activity cannot be submitted in its current status");
        }
        if (!hasSession(activity.getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_ARGUMENT, "Configure at least one activity session before submitting");
        }
        activity.setStatus(ActivityStatus.PENDING_REVIEW);
        activity.setReviewNote(null);
        activity.setReviewUserId(null);
        activity.setReviewTime(null);
        activityMapper.updateById(activity);
    }

    @Transactional
    public void approve(AuthenticatedPrincipal principal, Long id, String reviewNote) {
        requireAdmin(principal);
        Activity activity = requirePendingReview(id);
        LocalDateTime now = LocalDateTime.now(clock);
        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewNote(trimToNull(reviewNote));
        activity.setReviewUserId(principal.userId());
        activity.setReviewTime(now);
        activity.setPublishedTime(null);
        activityMapper.updateById(activity);
        recordReview(activity.getId(), principal.userId(), "APPROVED", reviewNote);
    }

    @Transactional
    public void reject(AuthenticatedPrincipal principal, Long id, String reviewNote) {
        requireAdmin(principal);
        if (trimToNull(reviewNote) == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "驳回活动时必须填写审核原因");
        }
        Activity activity = requirePendingReview(id);
        activity.setStatus(ActivityStatus.REJECTED);
        activity.setReviewNote(trimToNull(reviewNote));
        activity.setReviewUserId(principal.userId());
        activity.setReviewTime(LocalDateTime.now(clock));
        activityMapper.updateById(activity);
        recordReview(activity.getId(), principal.userId(), "REJECTED", reviewNote);
    }

    @Transactional
    public void publish(AuthenticatedPrincipal principal, Long id) {
        Activity activity = findRequired(id);
        requireCreator(principal, activity);
        if (activity.getStatus() != ActivityStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有审核通过的活动才能发布");
        }
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setPublishedTime(LocalDateTime.now(clock));
        activityMapper.updateById(activity);
    }

    @Transactional
    public void offline(AuthenticatedPrincipal principal, Long id, String reviewNote) {
        requireAdmin(principal);
        Activity activity = findRequired(id);
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        activity.setStatus(ActivityStatus.OFFLINE);
        activity.setReviewNote(trimToNull(reviewNote));
        activity.setReviewUserId(principal.userId());
        activity.setReviewTime(LocalDateTime.now(clock));
        activityMapper.updateById(activity);
        recordReview(activity.getId(), principal.userId(), "OFFLINE", reviewNote);
    }

    private Activity requirePendingReview(Long id) {
        Activity activity = findRequired(id);
        if (activity.getStatus() != ActivityStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return activity;
    }

    private Activity findRequired(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return activity;
    }

    private void requireCreator(AuthenticatedPrincipal principal, Activity activity) {
        if (!activity.getCreateUserId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireAdmin(AuthenticatedPrincipal principal) {
        if (!hasRole(principal, ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasRole(AuthenticatedPrincipal principal, String role) {
        return principal.roles().contains(role);
    }

    private boolean hasSession(Long activityId) {
        return activitySessionMapper.selectCount(
                        new LambdaQueryWrapper<ActivitySession>().eq(ActivitySession::getActivityId, activityId))
                > 0;
    }

    private void validateCommand(ActivityCommand command) {
        if (!command.registrationEndTime().isAfter(command.registrationStartTime())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Registration end time must be after start time");
        }
    }

    private void applyCommand(Activity activity, ActivityCommand command) {
        activity.setTitle(command.title().trim());
        activity.setSummary(trimToNull(command.summary()));
        activity.setCoverUrl(trimToNull(command.coverUrl()));
        activity.setVenueName(trimToNull(command.venueName()));
        activity.setOrganizerName(command.organizerName().trim());
        activity.setContactName(command.contactName().trim());
        activity.setContactMobile(trimToNull(command.contactMobile()));
        activity.setContactEmail(trimToNull(command.contactEmail()));
        activity.setRegistrationStartTime(command.registrationStartTime());
        activity.setRegistrationEndTime(command.registrationEndTime());
    }

    private void recordReview(Long activityId, Long reviewerUserId, String action, String reviewNote) {
        ActivityReviewRecord record = new ActivityReviewRecord();
        record.setActivityId(activityId);
        record.setReviewerUserId(reviewerUserId);
        record.setAction(action);
        record.setReviewNote(trimToNull(reviewNote));
        activityReviewRecordMapper.insert(record);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ActivityCommand(
            String title,
            String summary,
            String coverUrl,
            String venueName,
            String organizerName,
            String contactName,
            String contactMobile,
            String contactEmail,
            LocalDateTime registrationStartTime,
            LocalDateTime registrationEndTime) {}
}
