package com.eventflow.registration.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eventflow.activity.domain.ActivitySessionStatus;
import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.registration.domain.RegistrationStatus;
import com.eventflow.registration.infrastructure.persistence.ActivityRegistration;
import com.eventflow.registration.infrastructure.persistence.ActivityRegistrationMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final ActivityMapper activityMapper;
    private final ActivitySessionMapper activitySessionMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final Clock clock;

    public RegistrationService(
            ActivityMapper activityMapper,
            ActivitySessionMapper activitySessionMapper,
            ActivityRegistrationMapper registrationMapper,
            Clock clock) {
        this.activityMapper = activityMapper;
        this.activitySessionMapper = activitySessionMapper;
        this.registrationMapper = registrationMapper;
        this.clock = clock;
    }

    @Transactional
    public Long register(AuthenticatedPrincipal principal, RegistrationCommand command) {
        Activity activity = requireOpenActivity(command.activityId());
        ActivitySession session = requireAvailableSession(command, activity.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        validateRegistrationWindow(activity, now);
        if (!session.getStartTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该场次已经开始，无法报名");
        }

        ActivityRegistration existing = findByActivityAndUser(activity.getId(), principal.userId());
        if (existing != null && existing.getStatus() == RegistrationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经报名了该活动");
        }
        if (activitySessionMapper.confirmQuota(activity.getId(), session.getId()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "该场次名额已满");
        }

        if (existing != null) {
            if (registrationMapper.reactivate(existing.getId(), session.getId()) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "报名状态已发生变化，请刷新后重试");
            }
            return existing.getId();
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activity.getId());
        registration.setSessionId(session.getId());
        registration.setUserId(principal.userId());
        registration.setStatus(RegistrationStatus.CONFIRMED);
        try {
            registrationMapper.insert(registration);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已经报名了该活动");
        }
        return registration.getId();
    }

    @Transactional(readOnly = true)
    public List<RegistrationView> listMine(AuthenticatedPrincipal principal) {
        List<ActivityRegistration> registrations =
                registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, principal.userId())
                        .orderByDesc(ActivityRegistration::getUpdateTime)
                        .orderByDesc(ActivityRegistration::getId));
        if (registrations.isEmpty()) {
            return List.of();
        }

        Map<Long, Activity> activities = activityMapper
                .selectBatchIds(registrations.stream()
                        .map(ActivityRegistration::getActivityId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Activity::getId, Function.identity()));
        Map<Long, ActivitySession> sessions = activitySessionMapper
                .selectBatchIds(registrations.stream()
                        .map(ActivityRegistration::getSessionId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ActivitySession::getId, Function.identity()));

        return registrations.stream()
                .map(registration -> toView(registration, activities, sessions))
                .filter(view -> view != null)
                .toList();
    }

    @Transactional
    public void cancel(AuthenticatedPrincipal principal, Long registrationId) {
        ActivityRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!registration.getUserId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CONFLICT, "该报名已经取消");
        }
        if (activitySessionMapper.releaseConfirmedQuota(registration.getActivityId(), registration.getSessionId())
                != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "场次名额状态异常，请稍后重试");
        }
        if (registrationMapper.cancel(registration.getId(), principal.userId()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "报名状态已发生变化，请刷新后重试");
        }
    }

    private Activity requireOpenActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "该活动尚未发布或已下线");
        }
        return activity;
    }

    private ActivitySession requireAvailableSession(RegistrationCommand command, Long activityId) {
        ActivitySession session = activitySessionMapper.selectById(command.sessionId());
        if (session == null || !session.getActivityId().equals(activityId)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "所选场次不属于该活动");
        }
        if (session.getStatus() != ActivitySessionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "该场次不可报名");
        }
        return session;
    }

    private void validateRegistrationWindow(Activity activity, LocalDateTime now) {
        if (now.isBefore(activity.getRegistrationStartTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "报名尚未开始");
        }
        if (!now.isBefore(activity.getRegistrationEndTime())) {
            throw new BusinessException(ErrorCode.CONFLICT, "报名已经结束");
        }
    }

    private ActivityRegistration findByActivityAndUser(Long activityId, Long userId) {
        return registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
    }

    private RegistrationView toView(
            ActivityRegistration registration, Map<Long, Activity> activities, Map<Long, ActivitySession> sessions) {
        Activity activity = activities.get(registration.getActivityId());
        ActivitySession session = sessions.get(registration.getSessionId());
        if (activity == null || session == null) {
            return null;
        }
        return new RegistrationView(
                registration.getId(),
                registration.getActivityId(),
                registration.getSessionId(),
                registration.getStatus(),
                activity.getTitle(),
                activity.getVenueName(),
                session.getTitle(),
                session.getStartTime(),
                session.getEndTime(),
                registration.getCreateTime(),
                registration.getUpdateTime());
    }

    public record RegistrationCommand(Long activityId, Long sessionId) {}

    public record RegistrationView(
            Long id,
            Long activityId,
            Long sessionId,
            RegistrationStatus status,
            String activityTitle,
            String venueName,
            String sessionTitle,
            LocalDateTime sessionStartTime,
            LocalDateTime sessionEndTime,
            LocalDateTime createTime,
            LocalDateTime updateTime) {}
}
