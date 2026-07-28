package com.parkai.backend.controller;

import com.parkai.backend.dto.CreateParkingReportRequest;
import com.parkai.backend.dto.ParkingReportResponse;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.model.User;
import com.parkai.backend.repository.ParkingReportRepository;
import com.parkai.backend.repository.UserRepository;
import com.parkai.backend.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class ParkingReportController {

    private final ParkingReportRepository parkingReportRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ParkingReportController(
            ParkingReportRepository parkingReportRepository,
            UserRepository userRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.parkingReportRepository = parkingReportRepository;
        this.userRepository = userRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @PostMapping
    public ParkingReportResponse create(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid CreateParkingReportRequest request
    ) {
        Long userId = authenticatedUserProvider.getUserId(authorizationHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        ParkingReport report = new ParkingReport();
        report.setUser(user);
        report.setReportTime(LocalDateTime.now());
        report.setOccupancyPercent(request.occupancyPercent());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setStreetName(request.streetName());

        ParkingReport savedReport = parkingReportRepository.save(report);

        return new ParkingReportResponse(
                savedReport.getId(),
                savedReport.getLatitude(),
                savedReport.getLongitude(),
                savedReport.getStreetName(),
                savedReport.getUser().getId(),
                savedReport.getReportTime(),
                savedReport.getOccupancyPercent()
        );
    }
}