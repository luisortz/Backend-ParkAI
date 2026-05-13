package com.parkai.backend.service;

import com.parkai.backend.dto.NearbyStreet;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MapService {

    private final RestClient restClient;

    public MapService() {
        this.restClient = RestClient.create();
    }

    public List<NearbyStreet> getNearbyStreets(
            double latitude,
            double longitude
    ) {

        double radius = 500;

        String query = """
                [out:json];
                way
                  (around:%f,%f,%f)
                  ["highway"];
                out center tags;
                """.formatted(
                radius,
                latitude,
                longitude
        );

        Map response = restClient.post()
                .uri("https://overpass-api.de/api/interpreter")
                .body(query)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> elements =
                (List<Map<String, Object>>) response.get("elements");

        List<NearbyStreet> streets = new ArrayList<>();

        for (Map<String, Object> element : elements) {

            Map<String, Object> tags =
                    (Map<String, Object>) element.get("tags");

            if (tags == null || !tags.containsKey("name")) {
                continue;
            }

            Map<String, Object> center =
                    (Map<String, Object>) element.get("center");

            if (center == null) {
                continue;
            }

            String name = tags.get("name").toString();

            double lat =
                    Double.parseDouble(center.get("lat").toString());

            double lng =
                    Double.parseDouble(center.get("lon").toString());

            streets.add(
                    new NearbyStreet(
                            name,
                            lat,
                            lng
                    )
            );
        }

        return streets;
    }
}