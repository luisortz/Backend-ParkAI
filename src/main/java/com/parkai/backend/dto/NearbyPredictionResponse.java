package com.parkai.backend.dto;

public record NearbyPredictionResponse(

        String streetName,
        double latitude,
        double longitude,

        // Integer permite devolver null.
        Integer estimatedAvailabilityPercent,

        String level

) {
}