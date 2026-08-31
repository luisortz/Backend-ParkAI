package com.parkai.backend.service;

import com.parkai.backend.dto.NearbyStreet;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MapService {

    private final RestClient restClient;

    public MapService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5 segundos
        requestFactory.setReadTimeout(5000);    // 5 segundos

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public List<NearbyStreet> getNearbyStreets(
            double latitude,
            double longitude
    ) {
        double radius = 1000;

        String query = String.format(
                Locale.US,
                """
                [out:json][timeout:25];
                way
                  (around:%.0f,%.7f,%.7f)
                  ["highway"]
                  ["name"];
                out center tags;
                """,
                radius,
                latitude,
                longitude
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        try {
            Map<?, ?> response = restClient.post()
                    .uri("https://overpass-api.de/api/interpreter")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (response == null ||
                    !(response.get("elements") instanceof List<?> elements)) {
                return List.of();
            }

            List<NearbyStreet> streets = new ArrayList<>();

            for (Object object : elements) {

                if (!(object instanceof Map<?, ?> element)) {
                    continue;
                }

                if (!(element.get("tags") instanceof Map<?, ?> tags)) {
                    continue;
                }

                if (!(element.get("center") instanceof Map<?, ?> center)) {
                    continue;
                }

                Object nameValue = tags.get("name");
                Object latitudeValue = center.get("lat");
                Object longitudeValue = center.get("lon");

                if (nameValue == null ||
                        latitudeValue == null ||
                        longitudeValue == null) {
                    continue;
                }

                streets.add(
                        new NearbyStreet(
                                nameValue.toString(),
                                Double.parseDouble(latitudeValue.toString()),
                                Double.parseDouble(longitudeValue.toString())
                        )
                );
            }

            return streets.stream()
                    .distinct()
                    .limit(100)
                    .toList();

        } catch (Exception ex) {
            // Si Overpass falla (caído, timeout, error de red),
            // no tiramos abajo el endpoint completo: devolvemos vacío
            // y el resto del flujo sigue funcionando (heurística / community-reports).
            System.err.println("Error consultando Overpass API: " + ex.getMessage());
            return List.of();
        }
    }
}