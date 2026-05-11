package com.parkai.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateParkingReportRequest(
        @NotNull Long zoneId,
        Long userId,
        @Min(0) @Max(100) int occupancyPercent
) {
}