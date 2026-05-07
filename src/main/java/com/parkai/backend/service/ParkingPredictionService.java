package com.parkai.backend.service;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.repository.ParkingReportRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingPredictionService {

    private final ParkingReportRepository parkingReportRepository;

    public ParkingPredictionService(ParkingReportRepository parkingReportRepository) {
        this.parkingReportRepository = parkingReportRepository;
    }

    public PredictionResponse estimateAvailability(Long zoneId, int dayOfWeek, int hour) {
        validateInput(dayOfWeek, hour);

        LocalDateTime now = LocalDateTime.now();
        List<ParkingReport> history = parkingReportRepository.findByZoneIdAndReportTimeBetween(
                zoneId,
                now.minusDays(30),
                now
        );

        int averageOccupancy = history.stream()
                .filter(report -> report.getReportTime().getDayOfWeek().getValue() == dayOfWeek)
                .filter(report -> report.getReportTime().getHour() == hour)
                .mapToInt(ParkingReport::getOccupancyPercent)
                .average()
                .stream()
                .mapToInt(value -> (int) Math.round(value))
                .findFirst()
                .orElseGet(() -> heuristicOccupancy(dayOfWeek, hour));

        int estimatedAvailability = Math.max(0, 100 - averageOccupancy);
        return new PredictionResponse(zoneId, dayOfWeek, hour, estimatedAvailability,
                history.isEmpty() ? "heuristic" : "historical+heuristic");
    }

    private void validateInput(int dayOfWeek, int hour) {
        if (dayOfWeek < DayOfWeek.MONDAY.getValue() || dayOfWeek > DayOfWeek.SUNDAY.getValue()) {
            throw new IllegalArgumentException("dayOfWeek must be between 1 and 7");
        }
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
    }

    private int heuristicOccupancy(int dayOfWeek, int hour) {
        boolean peakHour = (hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 20);
        boolean weekday = dayOfWeek >= 1 && dayOfWeek <= 5;

        if (weekday && peakHour) {
            return 85;
        }
        if (weekday) {
            return 65;
        }
        return 50;
    }
}
