package com.parkai.backend.dto;

public record FavoriteResponse(

        Long id,

        String streetName,

        Double latitude,

        Double longitude

) {
}