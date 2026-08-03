package com.parkai.backend.controller;

import com.parkai.backend.dto.CreateParkingReportRequest;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.security.AuthenticatedUserProvider;
import com.parkai.backend.service.ParkingReportService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ParkingReportController {

    private final ParkingReportService parkingReportService;

    private final AuthenticatedUserProvider
            authenticatedUserProvider;

    public ParkingReportController(

            ParkingReportService parkingReportService,

            AuthenticatedUserProvider authenticatedUserProvider
    ) {

        this.parkingReportService =
                parkingReportService;

        this.authenticatedUserProvider =
                authenticatedUserProvider;
    }

    @PostMapping
    public ParkingReport create(

            @RequestHeader("Authorization")
            String authorizationHeader,

            @RequestBody
            @Valid
            CreateParkingReportRequest request
    ) {

        Long userId =
                authenticatedUserProvider
                        .getUserId(authorizationHeader);

        return parkingReportService.create(
                userId,
                request
        );
    }
}