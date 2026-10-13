package com.parkai.backend.dto;

import java.time.LocalDateTime;

public record MyReportResponse(
        Long id,
        String streetName,
        double latitude,
        double longitude,
        String reportType,
        LocalDateTime reportTime
) {
}