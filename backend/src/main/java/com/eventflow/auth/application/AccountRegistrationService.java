package com.eventflow.auth.application;

import com.eventflow.auth.api.dto.RegisterRequest;
import com.eventflow.auth.api.dto.RegistrationResponse;
import com.eventflow.auth.domain.OrganizerApplicationStatus;
import com.eventflow.auth.domain.RegistrationType;
import com.eventflow.auth.domain.UserStatus;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplication;
import com.eventflow.auth.infrastructure.persistence.OrganizerApplicationMapper;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountRegistrationService {
    private static final String USER_ROLE = "USER";

    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final OrganizerApplicationMapper organizerApplicationMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            OrganizerApplicationMapper organizerApplicationMapper,
            PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.organizerApplicationMapper = organizerApplicationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userAccountMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        validateOrganizerRequest(request);

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setMobile(trimToNull(request.mobile()));
        user.setEmail(trimToNull(request.email()));
        user.setStatus(UserStatus.ACTIVE);
        userAccountMapper.insert(user);
        grantRole(user.getId(), USER_ROLE);

        if (request.registrationType() == RegistrationType.PARTICIPANT) {
            return new RegistrationResponse(user.getId(), RegistrationType.PARTICIPANT.name(), null);
        }

        OrganizerApplication application = new OrganizerApplication();
        application.setUserId(user.getId());
        application.setOrganizationName(request.organizationName().trim());
        application.setOrganizationDescription(trimToNull(request.organizationDescription()));
        application.setContactName(request.contactName().trim());
        application.setContactMobile(trimToNull(request.mobile()));
        application.setContactEmail(trimToNull(request.email()));
        application.setStatus(OrganizerApplicationStatus.PENDING);
        organizerApplicationMapper.insert(application);
        return new RegistrationResponse(
                user.getId(),
                RegistrationType.ORGANIZER.name(),
                application.getStatus().name());
    }

    private void validateOrganizerRequest(RegisterRequest request) {
        if (request.registrationType() != RegistrationType.ORGANIZER) {
            return;
        }
        if (isBlank(request.organizationName()) || isBlank(request.contactName())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private void grantRole(Long userId, String roleCode) {
        Long roleId = userRoleMapper.findRoleIdByCode(roleCode);
        if (roleId == null) {
            throw new IllegalStateException("Required system role is unavailable: " + roleCode);
        }
        userRoleMapper.insertUserRole(userId, roleId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
