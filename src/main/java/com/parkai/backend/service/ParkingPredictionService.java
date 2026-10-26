package com.parkai.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.parkai.backend.dto.NearbyPredictionResponse;
import com.parkai.backend.dto.NearbyStreet;
import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.model.ReportType;
import com.parkai.backend.repository.ParkingReportRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingPredictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingPredictionService.class);

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
        this.mapService = mapService;
        this.searchHistoryService = searchHistoryService;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public PredictionResponse estimateAvailability(
            String streetName,
            double latitude,
            double longitude,
            int dayOfWeek,
            int hour
    ) {
        validateInput(dayOfWeek, hour);

        PredictionResponse modelPrediction = getPythonModelPrediction(
                latitude, longitude, dayOfWeek, hour
        );
        if (modelPrediction != null) {
            return modelPrediction;
        }

        LocalDateTime now = LocalDateTime.now();
        double radius = 0.002;

        List<ParkingReport> history = parkingReportRepository
                .findByLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
                        latitude - radius,
                        latitude + radius,
                        longitude - radius,
                        longitude + radius,
                        now.minusDays(30),
                        now
                );

        List<ParkingReport> matchingReports = history.stream()
                .filter(report -> report.getReportTime().getDayOfWeek().getValue() == dayOfWeek)
                .filter(report -> Math.abs(report.getReportTime().getHour() - hour) <= 1)
                .toList();

        if (matchingReports.isEmpty()) {
            int availability = Math.max(0, 100 - heuristicOccupancy(dayOfWeek, hour));
            return new PredictionResponse(
                    latitude, longitude, dayOfWeek, hour, availability,
                    calculateLevel(availability), "heuristic"
            );
        }

        long foundCount = matchingReports.stream()
                .filter(r -> r.getReportType() == ReportType.FOUND)
                .count();

        int availability = (int) Math.round(foundCount * 100.0 / matchingReports.size());
        return new PredictionResponse(
                latitude, longitude, dayOfWeek, hour, availability,
                calculateLevel(availability), "community-reports"
        );
    }

    private PredictionResponse getPythonModelPrediction(
            double latitude, double longitude, int dayOfWeek, int hour
    ) {
        if (mlServiceUrl == null || mlServiceUrl.isBlank()) {
            return null;
        }

        try {
            PythonPredictionResponse response = restClient.post()
                    .uri(mlServiceUrl)
                    .body(new PythonPredictionRequest(latitude, longitude, dayOfWeek, hour))
                    .retrieve()
                    .body(PythonPredictionResponse.class);

            if (response == null || response.estimatedAvailabilityPercent() == null) {
                return null;
            }

            String source = response.modelSource();
            // Aceptamos únicamente el contrato del nuevo clasificador.
            // Evitamos confundir una regresión antigua con probabilidad FOUND.
            if (!"community-ml-reports".equals(source)
                    || !"PROBABILITY_OF_FOUND".equals(response.predictionKind())) {
                LOGGER.warn("Flask devolvio un modelo/tipo incompatible: {}", source);
                return null;
            }

            int probabilityPercent = Math.max(0, Math.min(100,
                    response.estimatedAvailabilityPercent()));

            return new PredictionResponse(
                    latitude, longitude, dayOfWeek, hour,
                    probabilityPercent, calculateLevel(probabilityPercent),
                    "community-ml-reports"
            );

        } catch (RestClientResponseException ex) {
            // 422 es una respuesta prevista del clasificador fuera de su cobertura.
            if (ex.getStatusCode().value() == 422) {
                LOGGER.debug("Sin cobertura de modelo en {}, {}", latitude, longitude);
            } else {
                LOGGER.warn("Error HTTP desde modelo Python: {}", ex.getStatusCode());
            }
            return null;
        } catch (RuntimeException ex) {
            LOGGER.warn("Modelo Python no disponible: {}", ex.getMessage());
            return null;
        }
    }

    private void validateInput(int dayOfWeek, int hour) {
        if (dayOfWeek < DayOfWeek.MONDAY.getValue()
                || dayOfWeek > DayOfWeek.SUNDAY.getValue()) {
            throw new IllegalArgumentException("dayOfWeek must be between 1 and 7");
        }
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
    }

    private int heuristicOccupancy(int dayOfWeek, int hour) {
        boolean weekday = dayOfWeek >= 1 && dayOfWeek <= 5;
        boolean peak = (hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 20);
        if (weekday && peak) return 85;
        if (weekday) return 65;
        return 50;
    }

    private String calculateLevel(int availabilityPercent) {
        if (availabilityPercent <= 30) return "LOW";
        if (availabilityPercent <= 60) return "MEDIUM";
        return "HIGH";
    }

    private record PythonPredictionRequest(
            double lat,
            double lon,
            int dayOfWeek,
            int hour
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PythonPredictionResponse(
            Integer estimatedAvailabilityPercent,
            String modelSource,
            String predictionKind
    ) {}

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
            searchHistoryService.saveSearch(userId, placeName, latitude, longitude);
        }

        List<NearbyStreet> streets = mapService.getNearbyStreets(latitude, longitude);
        return streets.stream()
                .sorted((a, b) -> Double.compare(
                        calculateDistance(latitude, longitude, a.latitude(), a.longitude()),
                        calculateDistance(latitude, longitude, b.latitude(), b.longitude())
                ))
                .limit(20)
                .map(street -> {
                    PredictionResponse prediction = estimateAvailability(
                            street.streetName(), street.latitude(), street.longitude(),
                            dayOfWeek, hour
                    );
                    return new NearbyPredictionResponse(
                            street.streetName(), street.latitude(), street.longitude(),
                            prediction.estimatedAvailabilityPercent(),
                            prediction.level(), prediction.source()
                    );
                })
                .distinct()
                .toList();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = lat1 - lat2;
        double lonDistance = lon1 - lon2;
        return Math.sqrt(latDistance * latDistance + lonDistance * lonDistance);
    }
}
