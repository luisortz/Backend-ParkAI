package com.parkai.backend.dto;

public record PredictionResponse(
        Long zoneId,
        int dayOfWeek,
        int hour,
        int estimatedAvailabilityPercent,
        String source
) {
}
