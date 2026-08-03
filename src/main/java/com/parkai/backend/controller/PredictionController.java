package com.parkai.backend.controller;

import com.parkai.backend.dto.PredictionResponse;
import com.parkai.backend.service.ParkingPredictionService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.parkai.backend.dto.NearbyPredictionResponse;
import com.parkai.backend.security.AuthenticatedUserProvider;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final ParkingPredictionService parkingPredictionService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public PredictionController(ParkingPredictionService parkingPredictionService,AuthenticatedUserProvider authenticatedUserProvider) {
        this.parkingPredictionService = parkingPredictionService;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GetMapping
public PredictionResponse predict(

        @RequestParam String streetName,

        @RequestParam double latitude,

        @RequestParam double longitude,

        @RequestParam int dayOfWeek,

        @RequestParam int hour

) {

    return parkingPredictionService.estimateAvailability(
            streetName,
            latitude,
            longitude,
            dayOfWeek,
            hour
    );
}

@GetMapping("/nearby")
public List<NearbyPredictionResponse>
nearbyPredictions(

        @RequestHeader(
                value = "Authorization",
                required = false
        )
        String authorizationHeader,

        @RequestParam String placeName,

        @RequestParam double latitude,

        @RequestParam double longitude,

        @RequestParam int dayOfWeek,

        @RequestParam int hour
) {

    Long userId = null;

    if (authorizationHeader != null) {

        userId =
                authenticatedUserProvider
                        .getUserId(
                                authorizationHeader
                        );
    }

    return parkingPredictionService
            .getNearbyPredictions(
                    userId,
                    placeName,
                    latitude,
                    longitude,
                    dayOfWeek,
                    hour
            );
}
}
