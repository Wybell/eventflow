package com.eventflow.auth.api;

import com.eventflow.auth.application.UserProfileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/avatars")
public class AvatarController {

    private final UserProfileService userProfileService;

    public AvatarController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        Resource resource = new FileSystemResource(userProfileService.resolveAvatar(fileName));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(contentType(fileName))
                .body(resource);
    }

    private MediaType contentType(String fileName) {
        if (fileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (fileName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
