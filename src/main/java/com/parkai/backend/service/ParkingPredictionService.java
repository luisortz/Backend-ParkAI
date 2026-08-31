package com.parkai.backend.service;

import com.parkai.backend.dto.NearbyPredictionResponse;
import com.parkai.backend.dto.NearbyStreet;
import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.model.ReportType;
import com.parkai.backend.repository.ParkingReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingPredictionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ParkingPredictionService.class);

    private final ParkingReportRepository parkingReportRepository;
    private final RestClient restClient;
    private final MapService mapService;
    private final SearchHistoryService searchHistoryService;

    @Value("${parking.ml.service.url:}")
    private String mlServiceUrl;

    public ParkingPredictionService(
            ParkingReportRepository parkingReportRepository,
            MapService mapService,
            SearchHistoryService searchHistoryService
    ) {
        

        this.parkingReportRepository = parkingReportRepository;
        this.restClient = RestClient.create();
        this.mapService = mapService;
        this.searchHistoryService = searchHistoryService;
    }

    public PredictionResponse estimateAvailability(

            String streetName,

            double latitude,

            double longitude,

            int dayOfWeek,

            int hour

    ) {

        validateInput(dayOfWeek, hour);

        PredictionResponse modelPrediction =
                getPythonModelPrediction(
                        latitude,
                        longitude,
                        dayOfWeek,
                        hour
                );

        if (modelPrediction != null) {
            return modelPrediction;
        }

        LocalDateTime now = LocalDateTime.now();

        double radius = 0.005;

        List<ParkingReport> history =
                parkingReportRepository
                        .findByStreetNameIgnoreCaseAndLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
                                streetName,
                                latitude - radius,
                                latitude + radius,
                                longitude - radius,
                                longitude + radius,
                                now.minusDays(30),
                                now
                        );

        List<ParkingReport> matchingReports =
                history.stream()
                        .filter(report ->
                                report.getReportTime()
                                        .getDayOfWeek()
                                        .getValue() == dayOfWeek
                        )
                        .filter(report ->
                                report.getReportTime()
                                        .getHour() == hour
                        )
                        .toList();

        if (matchingReports.isEmpty()) {

            int estimatedAvailability =
                    Math.max(
                            0,
                            100 - heuristicOccupancy(
                                    dayOfWeek,
                                    hour
                            )
                    );

            return new PredictionResponse(
                    latitude,
                    longitude,
                    dayOfWeek,
                    hour,
                    estimatedAvailability,
                    calculateLevel(estimatedAvailability),
                    "heuristic"
            );
        }

        long foundCount =
                matchingReports.stream()
                        .filter(report ->
                                report.getReportType()
                                        == ReportType.FOUND
                        )
                        .count();

        int estimatedAvailability =
                (int) (
                        foundCount * 100.0
                                / matchingReports.size()
                );

        return new PredictionResponse(
                latitude,
                longitude,
                dayOfWeek,
                hour,
                estimatedAvailability,
                calculateLevel(
                        estimatedAvailability
                ),
                "community-reports"
        );
    }
    private PredictionResponse getPythonModelPrediction(
        double latitude,
        double longitude,
        int dayOfWeek,
        int hour
) {

    if (mlServiceUrl == null || mlServiceUrl.isBlank()) {
        return null;
    }

    try {

        PythonPredictionResponse response =
                restClient.post()
                        .uri(mlServiceUrl)
                        .body(
                                new PythonPredictionRequest(
                                        latitude,
                                        longitude,
                                        dayOfWeek,
                                        hour
                                )
                        )
                        .retrieve()
                        .body(PythonPredictionResponse.class);

        if (response == null) {
            return null;
        }

        int availability = Math.max(
                0,
                Math.min(
                        100,
                        response.estimatedAvailabilityPercent()
                )
        );

        return new PredictionResponse(
                latitude,
                longitude,
                dayOfWeek,
                hour,
                availability,
                calculateLevel(availability),
                "python-sklearn-service"
        );

    } catch (RuntimeException ex) {

        LOGGER.warn(
                "Python prediction failed: {}",
                ex.getMessage()
        );

        return null;
    }
}

private void validateInput(
        int dayOfWeek,
        int hour
) {

    if (dayOfWeek < DayOfWeek.MONDAY.getValue()
            || dayOfWeek > DayOfWeek.SUNDAY.getValue()) {

        throw new IllegalArgumentException(
                "dayOfWeek must be between 1 and 7"
        );
    }

    if (hour < 0 || hour > 23) {

        throw new IllegalArgumentException(
                "hour must be between 0 and 23"
        );
    }
}

private int heuristicOccupancy(
        int dayOfWeek,
        int hour
) {

    boolean weekday =
            dayOfWeek >= 1 && dayOfWeek <= 5;

    boolean peak =
            (hour >= 8 && hour <= 10)
                    || (hour >= 17 && hour <= 20);

    if (weekday && peak) {
        return 85;
    }

    if (weekday) {
        return 65;
    }

    return 50;
}

private String calculateLevel(
        int availabilityPercent
) {

    if (availabilityPercent <= 30) {
        return "LOW";
    }

    if (availabilityPercent <= 60) {
        return "MEDIUM";
    }

    return "HIGH";
}

private record PythonPredictionRequest(
        double latitude,
        double longitude,
        int dayOfWeek,
        int hour
) {
}

private record PythonPredictionResponse(
        int estimatedAvailabilityPercent
) {
}

public List<NearbyPredictionResponse> getNearbyPredictions(
        Long userId,
        String placeName,
        double latitude,
        double longitude,
        int dayOfWeek,
        int hour
) {

    validateInput(dayOfWeek, hour);

    if (userId != null) {
        searchHistoryService.saveSearch(
                userId,
                placeName,
                latitude,
                longitude
        );
    }

    List<NearbyStreet> streets =
            mapService.getNearbyStreets(
                    latitude,
                    longitude
            );

    return streets.stream()
            .sorted((a, b) -> Double.compare(
                    calculateDistance(
                            latitude,
                            longitude,
                            a.latitude(),
                            a.longitude()
                    ),
                    calculateDistance(
                            latitude,
                            longitude,
                            b.latitude(),
                            b.longitude()
                    )
            ))
            .limit(20)
            .map(street -> {

                PredictionResponse prediction =
                        estimateAvailability(
                                street.streetName(),
                                street.latitude(),
                                street.longitude(),
                                dayOfWeek,
                                hour
                        );

                return new NearbyPredictionResponse(
                        street.streetName(),
                        street.latitude(),
                        street.longitude(),
                        prediction.estimatedAvailabilityPercent(),
                        prediction.level(),
                        prediction.source()
                );
            })
            .distinct()
            .toList();
}

private double calculateDistance(
        double lat1,
        double lon1,
        double lat2,
        double lon2
) {

    double latDistance = lat1 - lat2;
    double lonDistance = lon1 - lon2;

    return Math.sqrt(
            latDistance * latDistance +
            lonDistance * lonDistance
    );
}
}
