package com.parkai.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateParkingReportRequest(

        double latitude,

        double longitude,

        @NotBlank
        String streetName,

        Long userId,

        @Min(0)
        @Max(100)
        int occupancyPercent

) {
}