package com.parkai.backend.dto;

public record UserResponse(
    Long id,
    String name,
    String email,
    String avatarKey,
    boolean hasProfileImage
) {}