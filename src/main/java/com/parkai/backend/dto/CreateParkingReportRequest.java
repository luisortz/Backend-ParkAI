package com.parkai.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateParkingReportRequest(
        @NotNull Long zoneId,
        Long userId,
        @NotNull LocalDateTime reportTime,
        @Min(0) @Max(100) int occupancyPercent
) {
}
