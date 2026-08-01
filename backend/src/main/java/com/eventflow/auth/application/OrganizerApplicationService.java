package com.eventflow.auth.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eventflow.auth.api.dto.OrganizerApplicationResponse;
import com.eventflow.auth.domain.OrganizerApplicationStatus;
import com.eventflow.auth.infrastructure.persistence.Organization;
import com.eventflow.auth.infrastructure.persistence.OrganizationMapper;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplication;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplicationMapper;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizerApplicationService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORGANIZER_ROLE = "ORGANIZER";

    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final OrganizationMapper organizationMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final Clock clock;

    public OrganizerApplicationService(
            OrganizerApplicationMapper organizerApplicationMapper,
            OrganizationMapper organizationMapper,
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            Clock clock) {
        this.organizerApplicationMapper = organizerApplicationMapper;
        this.organizationMapper = organizationMapper;
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrganizerApplicationResponse> listMine(AuthenticatedPrincipal principal) {
        return organizerApplicationMapper
                .selectList(new LambdaQueryWrapper<OrganizerApplication>()
                        .eq(OrganizerApplication::getUserId, principal.userId())
                        .orderByDesc(OrganizerApplication::getId))
                .stream()
                .map(OrganizerApplicationService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizerApplicationResponse> listPending(AuthenticatedPrincipal principal) {
        requireAdmin(principal);
        return organizerApplicationMapper
                .selectList(new LambdaQueryWrapper<OrganizerApplication>()
                        .eq(OrganizerApplication::getStatus, OrganizerApplicationStatus.PENDING)
                        .orderByAsc(OrganizerApplication::getId))
                .stream()
                .map(OrganizerApplicationService::toResponse)
                .toList();
    }

    @Transactional
    public void approve(AuthenticatedPrincipal principal, Long applicationId, String reviewNote) {
        requireAdmin(principal);
        OrganizerApplication application = findPending(applicationId);
        UserAccount user = userAccountMapper.selectById(application.getUserId());
        if (user == null || !user.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        Organization organization = new Organization();
        organization.setName(application.getOrganizationName());
        organization.setDescription(application.getOrganizationDescription());
        organization.setContactName(application.getContactName());
        organization.setContactMobile(application.getContactMobile());
        organization.setContactEmail(application.getContactEmail());
        organization.setStatus("ACTIVE");
        organizationMapper.insert(organization);

        userAccountMapper.updateOrganizationId(user.getId(), organization.getId());
        grantRole(user.getId(), ORGANIZER_ROLE);
        completeReview(application, OrganizerApplicationStatus.APPROVED, principal.userId(), reviewNote);
    }

    @Transactional
    public void reject(AuthenticatedPrincipal principal, Long applicationId, String reviewNote) {
        requireAdmin(principal);
        OrganizerApplication application = findPending(applicationId);
        completeReview(application, OrganizerApplicationStatus.REJECTED, principal.userId(), reviewNote);
    }

    private OrganizerApplication findPending(Long applicationId) {
        OrganizerApplication application = organizerApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (application.getStatus() != OrganizerApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return application;
    }

    private void completeReview(
            OrganizerApplication application, OrganizerApplicationStatus status, long reviewerId, String reviewNote) {
        application.setStatus(status);
        application.setReviewUserId(reviewerId);
        application.setReviewTime(LocalDateTime.now(clock));
        application.setReviewNote(trimToNull(reviewNote));
        organizerApplicationMapper.updateById(application);
    }

    private void grantRole(Long userId, String roleCode) {
        Long roleId = userRoleMapper.findRoleIdByCode(roleCode);
        if (roleId == null) {
            throw new IllegalStateException("Required system role is unavailable: " + roleCode);
        }
        if (!userRoleMapper.findRoleCodesByUserId(userId).contains(roleCode)) {
            userRoleMapper.insertUserRole(userId, roleId);
        }
    }

    private void requireAdmin(AuthenticatedPrincipal principal) {
        if (!principal.roles().contains(ADMIN_ROLE)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static OrganizerApplicationResponse toResponse(OrganizerApplication application) {
        return new OrganizerApplicationResponse(
                application.getId(),
                application.getOrganizationName(),
                application.getStatus().name(),
                application.getReviewNote(),
                application.getCreateTime(),
                application.getReviewTime());
    }
}
