package com.parkai.backend.service;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.model.ParkingReport;
import com.parkai.backend.repository.ParkingReportRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyDouble;

class ParkingPredictionServiceTest {

    @Test
void returnsHeuristicPredictionWhenThereIsNoHistory() {

    ParkingReportRepository repository = mock(ParkingReportRepository.class);

    when(repository.findByLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
    )).thenReturn(Collections.emptyList());

    ParkingPredictionService service = new ParkingPredictionService(repository);

    PredictionResponse response =
            service.estimateAvailability(-34.58, -58.42, 1, 9);

    assertEquals(15, response.estimatedAvailabilityPercent());

    assertEquals("heuristic", response.source());
}

    @Test
    void returnsHeuristicWhenHistoryExistsButNoMatchingSlot() {
        ParkingReportRepository repository = mock(ParkingReportRepository.class);

        ParkingReport nonMatching = new ParkingReport();
        nonMatching.setReportTime(LocalDateTime.of(2026, 5, 9, 20, 0));
        nonMatching.setOccupancyPercent(90);

        when(repository.findByLatitudeBetweenAndLongitudeBetweenAndReportTimeBetween(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
)).thenReturn(List.of(nonMatching));

ParkingPredictionService service = new ParkingPredictionService(repository);

PredictionResponse response =
        service.estimateAvailability(-34.58, -58.42, 1, 9);

assertEquals(15, response.estimatedAvailabilityPercent());

assertEquals("heuristic", response.source());
    }

    @Test
    void validatesDayRange() {
        ParkingReportRepository repository = mock(ParkingReportRepository.class);
        ParkingPredictionService service = new ParkingPredictionService(repository);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.estimateAvailability(-34.58, -58.42, 0, 10)
        );

        assertTrue(exception.getMessage().contains("dayOfWeek"));
    }
}
