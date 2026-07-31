package com.eventflow.activity.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eventflow.activity.domain.ActivityStatus;
import com.eventflow.activity.infrastructure.persistence.Activity;
import com.eventflow.activity.infrastructure.persistence.ActivityMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private static final String ORGANIZER_ROLE = "ORGANIZER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final ActivityMapper activityMapper;

    public ActivityService(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Transactional
    public Long create(AuthenticatedPrincipal principal, ActivityCommand command) {
        requireOrganizer(principal);
        validateRegistrationWindow(command.registrationStartTime(), command.registrationEndTime());

        Activity activity = new Activity();
        activity.setOrganizationId(principal.organizationId());
        activity.setCreateUserId(principal.userId());
        applyCommand(activity, command);
        activity.setStatus(ActivityStatus.DRAFT);
        activityMapper.insert(activity);
        return activity.getId();
    }

    @Transactional
    public void update(AuthenticatedPrincipal principal, Long id, ActivityCommand command) {
        requireOrganizer(principal);
        validateRegistrationWindow(command.registrationStartTime(), command.registrationEndTime());

        Activity activity = findRequired(id);
        requireOrganizerOwnership(principal, activity);
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        applyCommand(activity, command);
        activityMapper.updateById(activity);
    }

    @Transactional(readOnly = true)
    public List<Activity> listMine(AuthenticatedPrincipal principal) {
        requireOrganizer(principal);
        return activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizationId, principal.organizationId())
                .orderByDesc(Activity::getId));
    }

    @Transactional(readOnly = true)
    public Activity get(AuthenticatedPrincipal principal, Long id) {
        Activity activity = findRequired(id);
        if (!hasRole(principal, ADMIN_ROLE)) {
            requireOrganizerOwnership(principal, activity);
        }
        return activity;
    }

    @Transactional
    public void publish(AuthenticatedPrincipal principal, Long id) {
        requireOrganizer(principal);
        Activity activity = findRequired(id);
        requireOrganizerOwnership(principal, activity);
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        activity.setStatus(ActivityStatus.PUBLISHED);
        activityMapper.updateById(activity);
    }

    @Transactional
    public void offline(AuthenticatedPrincipal principal, Long id) {
        if (!hasRole(principal, ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Activity activity = findRequired(id);
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        activity.setStatus(ActivityStatus.OFFLINE);
        activityMapper.updateById(activity);
    }

    private Activity findRequired(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return activity;
    }

    private void requireOrganizer(AuthenticatedPrincipal principal) {
        if (!hasRole(principal, ORGANIZER_ROLE) || principal.organizationId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireOrganizerOwnership(AuthenticatedPrincipal principal, Activity activity) {
        if (!activity.getOrganizationId().equals(principal.organizationId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasRole(AuthenticatedPrincipal principal, String role) {
        return principal.roles().contains(role);
    }

    private void validateRegistrationWindow(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private void applyCommand(Activity activity, ActivityCommand command) {
        activity.setTitle(command.title().trim());
        activity.setSummary(trimToNull(command.summary()));
        activity.setCoverUrl(trimToNull(command.coverUrl()));
        activity.setVenueName(trimToNull(command.venueName()));
        activity.setRegistrationStartTime(command.registrationStartTime());
        activity.setRegistrationEndTime(command.registrationEndTime());
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
            LocalDateTime registrationStartTime,
            LocalDateTime registrationEndTime) {}
}
