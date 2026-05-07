package com.parkai.backend.controller;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.service.ParkingPredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final ParkingPredictionService parkingPredictionService;

    public PredictionController(ParkingPredictionService parkingPredictionService) {
        this.parkingPredictionService = parkingPredictionService;
    }

    @GetMapping
    public PredictionResponse predict(
            @RequestParam Long zoneId,
            @RequestParam int dayOfWeek,
            @RequestParam int hour
    ) {
        return parkingPredictionService.estimateAvailability(zoneId, dayOfWeek, hour);
    }
}
