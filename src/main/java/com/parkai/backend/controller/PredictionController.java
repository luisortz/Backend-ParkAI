package com.parkai.backend.controller;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.service.ParkingPredictionService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.parkai.backend.dto.NearbyPredictionResponse;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final ParkingPredictionService parkingPredictionService;

    public PredictionController(ParkingPredictionService parkingPredictionService) {
        this.parkingPredictionService = parkingPredictionService;
    }

    @GetMapping
public PredictionResponse predict(

        @RequestParam double latitude,

        @RequestParam double longitude,

        @RequestParam int dayOfWeek,

        @RequestParam int hour

) {

    return parkingPredictionService.estimateAvailability(
            latitude,
            longitude,
            dayOfWeek,
            hour
    );
}

@GetMapping("/nearby")
public List<NearbyPredictionResponse> nearbyPredictions(

        @RequestParam double latitude,

        @RequestParam double longitude,

        @RequestParam int dayOfWeek,

        @RequestParam int hour
) {

    return parkingPredictionService.getNearbyPredictions(
            latitude,
            longitude,
            dayOfWeek,
            hour
    );
}
}
