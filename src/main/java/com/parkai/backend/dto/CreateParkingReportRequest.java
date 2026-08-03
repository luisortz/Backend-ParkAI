package com.parkai.backend.dto;

import com.parkai.backend.model.ReportType;

public record CreateParkingReportRequest(

        String streetName,

        double latitude,

        double longitude,

        ReportType reportType

) {
}