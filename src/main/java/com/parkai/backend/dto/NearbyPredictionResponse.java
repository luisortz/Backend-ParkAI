package com.parkai.backend.dto;

public record NearbyPredictionResponse(

        String streetName,

        double latitude,

        double longitude,

        int estimatedAvailabilityPercent,

        String level,
        
        String source

) {
}