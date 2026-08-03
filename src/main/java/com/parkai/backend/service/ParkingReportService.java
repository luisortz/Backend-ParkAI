package com.parkai.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.parkai.backend.dto.CreateParkingReportRequest;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.model.User;
import com.parkai.backend.repository.ParkingReportRepository;
import com.parkai.backend.repository.UserRepository;

@Service
public class ParkingReportService {

    private final ParkingReportRepository parkingReportRepository;

    private final UserRepository userRepository;

    public ParkingReportService(
            ParkingReportRepository parkingReportRepository,
            UserRepository userRepository
    ) {
        this.parkingReportRepository =
                parkingReportRepository;

        this.userRepository = userRepository;
    }

    public ParkingReport create(
            Long userId,
            CreateParkingReportRequest request
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow();

        ParkingReport report =
                new ParkingReport();

        report.setUser(user);

        report.setReportTime(
                LocalDateTime.now()
        );

        report.setStreetName(
                request.streetName()
        );

        report.setLatitude(
                request.latitude()
        );

        report.setLongitude(
                request.longitude()
        );

        report.setReportType(
            request.reportType()
        );

        return parkingReportRepository.save(report);
    }
}
