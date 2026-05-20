package com.parkai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(

        @NotBlank
        String streetName,

        @NotNull
        Double latitude,

        @NotNull
        Double longitude

) {
}