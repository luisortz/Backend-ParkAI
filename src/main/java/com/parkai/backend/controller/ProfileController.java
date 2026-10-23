
package com.parkai.backend.controller;

import com.parkai.backend.security.AuthenticatedUserProvider;
import com.parkai.backend.service.UserProfileService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.parkai.backend.dto.ProfileStatsResponse;
import com.parkai.backend.service.ProfileStatsService;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileService profileService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ProfileStatsService profileStatsService;

    public ProfileController(
        UserProfileService profileService,
        AuthenticatedUserProvider authenticatedUserProvider,
        ProfileStatsService profileStatsService
) {
    this.profileService = profileService;
    this.authenticatedUserProvider = authenticatedUserProvider;
    this.profileStatsService = profileStatsService;
}

    @PostMapping(
            value = "/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadPhoto(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        Long userId = authenticatedUserProvider
                .getUserId(authorizationHeader);

        profileService.savePhoto(userId, file);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/avatar")
    public ResponseEntity<Void> selectAvatar(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody AvatarRequest request
    ) {
        Long userId = authenticatedUserProvider
                .getUserId(authorizationHeader);

        profileService.selectAvatar(
                userId,
                request.avatarKey()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPhoto(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        Long userId = authenticatedUserProvider
                .getUserId(authorizationHeader);

        byte[] photo = profileService.getPhoto(userId);

        String contentType = profileService
                .getPhotoContentType(userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Cache-Control", "private, no-store")
                .body(photo);
    }

    public record AvatarRequest(String avatarKey) {}

    @GetMapping("/stats")
public ProfileStatsResponse getStats(
        @RequestHeader("Authorization") String authorizationHeader
) {
    Long userId = authenticatedUserProvider
            .getUserId(authorizationHeader);

    return profileStatsService.getStats(userId);
}
}
