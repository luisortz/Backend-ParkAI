package com.parkai.backend.controller;

import com.parkai.backend.dto.CreateParkingReportRequest;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.model.User;
import com.parkai.backend.model.Zone;
import com.parkai.backend.repository.ParkingReportRepository;
import com.parkai.backend.repository.UserRepository;
import com.parkai.backend.repository.ZoneRepository;
import jakarta.validation.Valid;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports")
public class ParkingReportController {

    private final ParkingReportRepository parkingReportRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;

    public ParkingReportController(
            ParkingReportRepository parkingReportRepository,
            ZoneRepository zoneRepository,
            UserRepository userRepository
    ) {
        this.parkingReportRepository = parkingReportRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ParkingReport create(@RequestBody @Valid CreateParkingReportRequest request) {
        User user = null;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }

        ParkingReport report = new ParkingReport();
        report.setUser(user);
        report.setReportTime(LocalDateTime.now());
        report.setOccupancyPercent(request.occupancyPercent());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setStreetName(request.streetName());
        return parkingReportRepository.save(report);
    }
}
