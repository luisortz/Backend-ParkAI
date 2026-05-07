package com.parkai.backend.service;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.repository.ParkingReportRepository;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParkingPredictionServiceTest {

    @Test
    void returnsHeuristicPredictionWhenThereIsNoHistory() {
        ParkingReportRepository repository = mock(ParkingReportRepository.class);
        when(repository.findByZoneIdAndReportTimeBetween(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        ParkingPredictionService service = new ParkingPredictionService(repository);
        PredictionResponse response = service.estimateAvailability(1L, 1, 9);

        assertEquals(15, response.estimatedAvailabilityPercent());
        assertEquals("heuristic", response.source());
    }

    @Test
    void validatesDayRange() {
        ParkingReportRepository repository = mock(ParkingReportRepository.class);
        ParkingPredictionService service = new ParkingPredictionService(repository);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.estimateAvailability(1L, 0, 10)
        );

        assertTrue(exception.getMessage().contains("dayOfWeek"));
    }
}
