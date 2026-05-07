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

    @PostMapping
    public Zone create(@RequestBody @Valid Zone zone) {
        return zoneRepository.save(zone);
    }
}
