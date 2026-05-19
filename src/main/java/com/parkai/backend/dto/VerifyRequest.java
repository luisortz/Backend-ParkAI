package com.parkai.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(

        @Email
        String email,

        @NotBlank
        String code

) {
}