package com.parkai.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    private String verificationCode;

    private LocalDateTime verificationCodeExpiresAt;

    private LocalDateTime lastVerificationCodeSentAt;

    @Column(name = "avatar_key", length = 50)
    private String avatarKey = "default";

    @Lob
    @Column(name = "profile_image", columnDefinition = "LONGBLOB")
    private byte[] profileImage;

    @Column(name = "profile_image_content_type", length = 30)
    private String profileImageContentType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password=password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled=enabled;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode=verificationCode;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public void setVerificationCodeExpiresAt(LocalDateTime verificationCodeExpiresAt) {
        this.verificationCodeExpiresAt=verificationCodeExpiresAt;
    }

    public LocalDateTime getLastVerificationCodeSentAt() {
        return lastVerificationCodeSentAt;
    }
    
    public void setLastVerificationCodeSentAt(LocalDateTime lastVerificationCodeSentAt) {
        this.lastVerificationCodeSentAt =
                lastVerificationCodeSentAt;
    }

    public String getAvatarKey() {
        return avatarKey;
    }
    
    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }
    
    public byte[] getProfileImage() {
        return profileImage;
    }
    
    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }
    
    public String getProfileImageContentType() {
        return profileImageContentType;
    }
    
    public void setProfileImageContentType(String profileImageContentType) {
        this.profileImageContentType = profileImageContentType;
    }


}
