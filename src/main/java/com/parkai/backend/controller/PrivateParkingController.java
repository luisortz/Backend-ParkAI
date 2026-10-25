package com.parkai.backend.controller;

import com.parkai.backend.dto.PrivateParkingResponse;
import com.parkai.backend.service.PrivateParkingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking/private")
public class PrivateParkingController {
    private final PrivateParkingService service;

    public PrivateParkingController(PrivateParkingService service) {
        this.service = service;
    }

    @GetMapping("/nearby")
    public List<PrivateParkingResponse> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return service.findNearby(latitude, longitude);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidCoordinates(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
