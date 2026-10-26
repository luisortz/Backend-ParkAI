package com.parkai.backend.controller;

import com.parkai.backend.dto.StreetSegmentResponse;
import com.parkai.backend.service.CabaStreetSegmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class CabaStreetSegmentController {

    private final CabaStreetSegmentService service;

    public CabaStreetSegmentController(CabaStreetSegmentService service) {
        this.service = service;
    }

    @GetMapping("/nearby-segments")
    public List<StreetSegmentResponse> getNearbySegments(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return service.getNearbySegments(latitude, longitude);
    }
}
