package com.parkai.backend.dto;

public record PredictionResponse(

        double latitude,
        double longitude,
        int dayOfWeek,
        int hour,

        // Integer permite devolver null cuando no existen datos suficientes.
        Integer estimatedAvailabilityPercent,

        String level,
        String source

) {
}