package com.parkai.backend.service;

import com.parkai.backend.dto.NearbyPredictionResponse;
import com.parkai.backend.dto.NearbyStreet;
import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.model.ParkingReport;
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

    /*
     * No se genera una predicción histórica con uno o dos reportes,
     * porque sería poco confiable.
     */
    private static final int MINIMUM_REPORTS_REQUIRED = 3;

    /*
     * Aproximadamente 300 metros alrededor del punto consultado.
     */
    private static final double SEARCH_RADIUS = 0.003;

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
            double latitude,
            double longitude,
            int dayOfWeek,
            int hour
    ) {
        validateInput(dayOfWeek, hour);

        /*
         * Primera opción: usar el modelo real de Python.
         */
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

        /*
         * Segunda opción: usar reportes reales guardados en MySQL.
         */
        LocalDateTime now = LocalDateTime.now();

        List<ParkingReport> history =
                parkingReportRepository
                        .findByLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
                                latitude - SEARCH_RADIUS,
                                latitude + SEARCH_RADIUS,
                                longitude - SEARCH_RADIUS,
                                longitude + SEARCH_RADIUS,
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

        /*
         * No se inventa un resultado cuando faltan reportes.
         */
        if (matchingReports.size() < MINIMUM_REPORTS_REQUIRED) {
            return insufficientDataResponse(
                    latitude,
                    longitude,
                    dayOfWeek,
                    hour
            );
        }

        double averageOccupancy =
                matchingReports.stream()
                        .mapToInt(ParkingReport::getOccupancyPercent)
                        .average()
                        .orElseThrow();

        int estimatedAvailability =
                Math.max(
                        0,
                        Math.min(
                                100,
                                100 - (int) Math.round(averageOccupancy)
                        )
                );

        return new PredictionResponse(
                latitude,
                longitude,
                dayOfWeek,
                hour,
                estimatedAvailability,
                calculateLevel(estimatedAvailability),
                "historical-reports"
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

            int availability =
                    Math.max(
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

        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "No se pudo consultar el servicio de IA: {}",
                    exception.getMessage()
            );

            return null;
        }
    }

    private PredictionResponse insufficientDataResponse(
            double latitude,
            double longitude,
            int dayOfWeek,
            int hour
    ) {
        return new PredictionResponse(
                latitude,
                longitude,
                dayOfWeek,
                hour,
                null,
                "UNKNOWN",
                "insufficient-data"
        );
    }

    private void validateInput(
            int dayOfWeek,
            int hour
    ) {
        if (
                dayOfWeek < DayOfWeek.MONDAY.getValue()
                        || dayOfWeek > DayOfWeek.SUNDAY.getValue()
        ) {
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
                .map(street -> {
                    PredictionResponse prediction =
                            estimateAvailability(
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
                            prediction.level()
                    );
                })
                .toList();
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
}