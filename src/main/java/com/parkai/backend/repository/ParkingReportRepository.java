package com.parkai.backend.repository;

import com.parkai.backend.model.ParkingReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ParkingReportRepository extends JpaRepository<ParkingReport, Long> {

    List<ParkingReport> findByLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
            double minLat,
            double maxLat,
            double minLng,
            double maxLng,
            LocalDateTime start,
            LocalDateTime end
    );

    List<ParkingReport>
findByStreetNameIgnoreCaseAndLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
        String streetName,
        double minLat,
        double maxLat,
        double minLon,
        double maxLon,
        LocalDateTime start,
        LocalDateTime end
);
}