package com.parkai.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    @Value("${parking.dataset.url}")
    private String datasetUrl;

    @GetMapping("/parking")
    public Map<String, String> parkingDatasetSource() {
        return Map.of(
                "city", "Buenos Aires",
                "name", "Estacionamiento en la vía pública",
                "url", datasetUrl
        );
    }
}
