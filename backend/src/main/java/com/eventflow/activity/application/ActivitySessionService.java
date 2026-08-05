package com.eventflow.activity.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eventflow.activity.domain.ActivitySessionStatus;
import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.activity.infrastructure.persistence.ActivitySession;
import com.eventflow.activity.infrastructure.persistence.ActivitySessionMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivitySessionService {
    private final ActivityMapper activityMapper;
    private final ActivitySessionMapper activitySessionMapper;

    public ActivitySessionService(ActivityMapper activityMapper, ActivitySessionMapper activitySessionMapper) {
        this.activityMapper = activityMapper;
        this.activitySessionMapper = activitySessionMapper;
    }

    @Transactional
    public Long create(AuthenticatedPrincipal principal, Long activityId, SessionCommand command) {
        Activity activity = requireDraftActivityOwnership(principal, activityId);
        validateCommand(command);

        ActivitySession session = new ActivitySession();
        session.setActivityId(activity.getId());
        session.setTitle(command.title().trim());
        session.setStartTime(command.startTime());
        session.setEndTime(command.endTime());
        session.setTotalQuota(command.totalQuota());
        session.setAvailableQuota(command.totalQuota());
        session.setReservedQuota(0);
        session.setConfirmedQuota(0);
        session.setStatus(ActivitySessionStatus.ACTIVE);
        activitySessionMapper.insert(session);
        return session.getId();
    }

    @Transactional(readOnly = true)
    public List<ActivitySession> list(AuthenticatedPrincipal principal, Long activityId) {
        requireActivityOwnership(principal, activityId);
        return activitySessionMapper.selectList(new LambdaQueryWrapper<ActivitySession>()
                .eq(ActivitySession::getActivityId, activityId)
                .orderByAsc(ActivitySession::getStartTime)
                .orderByAsc(ActivitySession::getId));
    }

    @Transactional(readOnly = true)
    public List<ActivitySession> listPublished(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return activitySessionMapper.selectList(new LambdaQueryWrapper<ActivitySession>()
                .eq(ActivitySession::getActivityId, activityId)
                .eq(ActivitySession::getStatus, ActivitySessionStatus.ACTIVE)
                .orderByAsc(ActivitySession::getStartTime)
                .orderByAsc(ActivitySession::getId));
    }

    private Activity requireDraftActivityOwnership(AuthenticatedPrincipal principal, Long activityId) {
        Activity activity = requireActivityOwnership(principal, activityId);
        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return activity;
    }

    private Activity requireActivityOwnership(AuthenticatedPrincipal principal, Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!activity.getCreateUserId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return activity;
    }

    private void validateCommand(SessionCommand command) {
        if (!command.endTime().isAfter(command.startTime()) || command.totalQuota() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    public record SessionCommand(String title, LocalDateTime startTime, LocalDateTime endTime, int totalQuota) {}
}
