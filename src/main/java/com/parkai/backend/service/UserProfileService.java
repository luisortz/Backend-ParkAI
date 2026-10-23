
package com.parkai.backend.service;

import com.parkai.backend.model.User;
import com.parkai.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
public class UserProfileService {

    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_AVATARS = Set.of(
            "default",
            "avatar_1",
            "avatar_2",
            "avatar_3",
            "avatar_4",
            "avatar_5",
            "avatar_6"
    );

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"
                ));
    }

    public void savePhoto(Long userId, MultipartFile file)
            throws IOException {

        if (file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La imagen debe pesar menos de 2 MB"
            );
        }

        byte[] bytes = file.getBytes();
        String contentType = detectImageType(bytes);

        User user = findUser(userId);
        user.setProfileImage(bytes);
        user.setProfileImageContentType(contentType);
        user.setAvatarKey(null);

        userRepository.save(user);
    }

    public void selectAvatar(Long userId, String avatarKey) {
        if (avatarKey == null || !ALLOWED_AVATARS.contains(avatarKey)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Avatar no válido"
            );
        }

        User user = findUser(userId);

        user.setAvatarKey(avatarKey);
        user.setProfileImage(null);
        user.setProfileImageContentType(null);

        userRepository.save(user);
    }

    public byte[] getPhoto(Long userId) {
        User user = findUser(userId);

        if (user.getProfileImage() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El usuario no tiene foto personalizada"
            );
        }

        return user.getProfileImage();
    }

    public String getPhotoContentType(Long userId) {
        User user = findUser(userId);

        if (user.getProfileImageContentType() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El usuario no tiene foto personalizada"
            );
        }

        return user.getProfileImageContentType();
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return "image/png";
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se permiten imágenes JPEG o PNG"
        );
    }
}
