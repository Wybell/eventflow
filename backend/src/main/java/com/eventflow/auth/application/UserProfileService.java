package com.eventflow.auth.application;

import com.eventflow.auth.api.dto.ChangePasswordRequest;
import com.eventflow.auth.api.dto.CurrentUserResponse;
import com.eventflow.auth.api.dto.UpdateProfileRequest;
import com.eventflow.auth.infrastructure.persistence.RefreshTokenMapper;
import com.eventflow.auth.infrastructure.persistence.UserAccount;
import com.eventflow.auth.infrastructure.persistence.UserAccountMapper;
import com.eventflow.auth.infrastructure.persistence.UserRoleMapper;
import com.eventflow.shared.error.BusinessException;
import com.eventflow.shared.error.ErrorCode;
import com.eventflow.shared.security.AuthenticatedPrincipal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;

    private final UserAccountMapper userAccountMapper;
    private final UserRoleMapper userRoleMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Path avatarDirectory;

    public UserProfileService(
            UserAccountMapper userAccountMapper,
            UserRoleMapper userRoleMapper,
            RefreshTokenMapper refreshTokenMapper,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${eventflow.storage.avatar-directory}") String avatarDirectory) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleMapper = userRoleMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.avatarDirectory = Path.of(avatarDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse get(AuthenticatedPrincipal principal) {
        return toResponse(requireActiveUser(principal));
    }

    @Transactional
    public CurrentUserResponse update(AuthenticatedPrincipal principal, UpdateProfileRequest request) {
        UserAccount user = requireActiveUser(principal);
        String mobile = trimToNull(request.mobile());
        String email = trimToNull(request.email());
        ensureContactIsAvailable(user.getId(), mobile, email);
        user.setDisplayName(request.displayName().trim());
        user.setMobile(mobile);
        user.setEmail(email);
        userAccountMapper.updateById(user);
        return toResponse(user);
    }

    @Transactional
    public String updateAvatar(AuthenticatedPrincipal principal, MultipartFile file) {
        UserAccount user = requireActiveUser(principal);
        try {
            byte[] content = file.getBytes();
            AvatarType avatarType = validateAvatar(file, content);
            String fileName = UUID.randomUUID() + avatarType.extension();
            Path target = avatarDirectory.resolve(fileName).normalize();
            if (!target.startsWith(avatarDirectory)) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
            }
            Files.createDirectories(avatarDirectory);
            Files.write(target, content);
            String avatarUrl = "/api/v1/avatars/" + fileName;
            String previousAvatarUrl = user.getAvatarUrl();
            user.setAvatarUrl(avatarUrl);
            userAccountMapper.updateById(user);
            deletePreviousAvatar(previousAvatarUrl);
            return avatarUrl;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像保存失败，请稍后重试");
        }
    }

    @Transactional
    public void changePassword(AuthenticatedPrincipal principal, ChangePasswordRequest request) {
        UserAccount user = requireActiveUser(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "新密码不能与当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userAccountMapper.updateById(user);
        refreshTokenMapper.revokeAllByUserId(user.getId(), LocalDateTime.now(clock));
    }

    public Path resolveAvatar(String fileName) {
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Path avatar = avatarDirectory.resolve(fileName).normalize();
        if (!avatar.startsWith(avatarDirectory) || !Files.isRegularFile(avatar)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return avatar;
    }

    private UserAccount requireActiveUser(AuthenticatedPrincipal principal) {
        UserAccount user = userAccountMapper.selectById(principal.userId());
        if (user == null || !user.isActive()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private CurrentUserResponse toResponse(UserAccount user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getMobile(),
                user.getEmail(),
                user.getOrganizationId(),
                Set.copyOf(userRoleMapper.findRoleCodesByUserId(user.getId())));
    }

    private void ensureContactIsAvailable(Long userId, String mobile, String email) {
        UserAccount mobileOwner = mobile == null ? null : userAccountMapper.findByMobile(mobile);
        if (mobileOwner != null && !mobileOwner.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该手机号已被其他账号使用");
        }
        UserAccount emailOwner = email == null ? null : userAccountMapper.findByEmail(email);
        if (emailOwner != null && !emailOwner.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该邮箱已被其他账号使用");
        }
    }

    private AvatarType validateAvatar(MultipartFile file, byte[] content) {
        if (file.isEmpty() || file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择不超过 5MB 的头像图片");
        }
        AvatarType avatarType = AvatarType.fromContentType(file.getContentType());
        if (!avatarType.matches(content)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "头像文件内容与图片格式不匹配");
        }
        return avatarType;
    }

    private void deletePreviousAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/api/v1/avatars/")) {
            return;
        }
        String fileName = avatarUrl.substring("/api/v1/avatars/".length());
        Path previousAvatar = avatarDirectory.resolve(fileName).normalize();
        if (!previousAvatar.startsWith(avatarDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(previousAvatar);
        } catch (IOException ignored) {
            // The new avatar is already persisted; stale file cleanup can be retried operationally.
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private enum AvatarType {
        JPEG("image/jpeg", ".jpg") {
            @Override
            boolean matches(byte[] content) {
                return content.length >= 3
                        && content[0] == (byte) 0xff
                        && content[1] == (byte) 0xd8
                        && content[2] == (byte) 0xff;
            }
        },
        PNG("image/png", ".png") {
            @Override
            boolean matches(byte[] content) {
                byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
                return startsWith(content, signature, 0);
            }
        },
        WEBP("image/webp", ".webp") {
            @Override
            boolean matches(byte[] content) {
                return startsWith(content, new byte[] {0x52, 0x49, 0x46, 0x46}, 0)
                        && startsWith(content, new byte[] {0x57, 0x45, 0x42, 0x50}, 8);
            }
        };

        private final String contentType;
        private final String extension;

        AvatarType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        private String extension() {
            return extension;
        }

        abstract boolean matches(byte[] content);

        private static boolean startsWith(byte[] content, byte[] signature, int offset) {
            if (content.length < offset + signature.length) {
                return false;
            }
            for (int index = 0; index < signature.length; index++) {
                if (content[offset + index] != signature[index]) {
                    return false;
                }
            }
            return true;
        }

        private static AvatarType fromContentType(String contentType) {
            if (contentType != null) {
                String normalized = contentType.toLowerCase(Locale.ROOT);
                for (AvatarType type : values()) {
                    if (type.contentType.equals(normalized)) {
                        return type;
                    }
                }
            }
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "头像仅支持 JPG、PNG 或 WEBP 图片");
        }
    }
}
