package com.parkai.backend.dto;

import java.time.LocalDateTime;

public record ParkingReportResponse(

        Long id,
        double latitude,
        double longitude,
        String streetName,
        Long userId,
        LocalDateTime reportTime,
        int occupancyPercent

) {
}