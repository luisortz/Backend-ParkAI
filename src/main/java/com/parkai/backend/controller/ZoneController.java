package com.parkai.backend.controller;

import com.parkai.backend.model.Zone;
import com.parkai.backend.repository.ZoneRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ZoneRepository zoneRepository;

    public ZoneController(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @GetMapping
    public List<Zone> list() {
        return zoneRepository.findAll();
    }

    @GetMapping("/nearby")
public List<Zone> nearby(
        @RequestParam double lat,
        @RequestParam double lng
) {

    double radius = 0.01;

    return zoneRepository.findByLatitudeBetweenAndLongitudeBetween(
            lat - radius,
            lat + radius,
            lng - radius,
            lng + radius
    );
}

    @PostMapping
    public Zone create(@RequestBody @Valid Zone zone) {
        return zoneRepository.save(zone);
    }
}
