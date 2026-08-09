package com.eventflow.auth.application;

import com.eventflow.auth.domain.UserStatus;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first administrator only when deployment-time credentials are explicitly provided. */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminBootstrapRunner(
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            @Value("${eventflow.bootstrap-admin.username:}") String username,
            @Value("${eventflow.bootstrap-admin.password:}") String password,
            @Value("${eventflow.bootstrap-admin.display-name:}") String displayName) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username.trim();
        this.password = password;
        this.displayName = displayName == null ? "" : displayName.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isEmpty() && password.isEmpty()) {
            LOGGER.info("Administrator bootstrap is disabled; no bootstrap credentials were provided");
            return;
        }
        validateConfiguration();

        UserAccount user = userAccountMapper.findByUsername(username);
        if (user == null) {
            user = createUser();
            userAccountMapper.insert(user);
            LOGGER.info("Created bootstrap administrator account username={}", username);
        } else if (!user.isActive()) {
            throw new IllegalStateException("Bootstrap administrator account is inactive: " + username);
        }

        grantAdminRoleIfMissing(user.getId());
    }

    private UserAccount createUser() {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName.isEmpty() ? username : displayName);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private void grantAdminRoleIfMissing(Long userId) {
        List<String> roles = userRoleMapper.findRoleCodesByUserId(userId);
        if (roles.contains(ADMIN_ROLE)) {
            return;
        }
        Long roleId = userRoleMapper.findRoleIdByCode(ADMIN_ROLE);
        if (roleId == null) {
            throw new IllegalStateException("Required system role is unavailable: " + ADMIN_ROLE);
        }
        userRoleMapper.insertUserRole(userId, roleId);
        LOGGER.info("Granted administrator role to username={}", username);
    }

    private void validateConfiguration() {
        if (username.isEmpty()) {
            throw new IllegalStateException("Bootstrap administrator username is required");
        }
        if (username.length() > 64) {
            throw new IllegalStateException("Bootstrap administrator username must be 64 characters or fewer");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("Bootstrap administrator password must be at least 8 characters");
        }
        if (displayName.length() > 64) {
            throw new IllegalStateException("Bootstrap administrator display name must be 64 characters or fewer");
        }
    }
}
