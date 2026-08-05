package com.eventflow.auth.application;

import com.eventflow.auth.api.dto.RegisterRequest;
import com.eventflow.auth.api.dto.RegistrationResponse;
import com.eventflow.auth.domain.UserStatus;
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
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(
            UserAccountMapper userAccountMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String mobile = trimToNull(request.mobile());
        String email = trimToNull(request.email());
        ensureAvailable(username, mobile, email);

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setMobile(mobile);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        userAccountMapper.insert(user);
        grantUserRole(user.getId());
        return new RegistrationResponse(user.getId());
    }

    private void ensureAvailable(String username, String mobile, String email) {
        if (userAccountMapper.findByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该账号已注册，请直接登录");
        }
        if (mobile != null && userAccountMapper.findByMobile(mobile) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已绑定其他账号");
        }
        if (email != null && userAccountMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "该邮箱已绑定其他账号");
        }
    }

    private void grantUserRole(Long userId) {
        Long roleId = userRoleMapper.findRoleIdByCode(USER_ROLE);
        if (roleId == null) {
            throw new IllegalStateException("Required system role is unavailable: " + USER_ROLE);
        }
        userRoleMapper.insertUserRole(userId, roleId);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
