package com.parkai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkai.backend.dto.NearbyStreet;
import com.parkai.backend.model.CabaStreetPoint;
import com.parkai.backend.model.StreetCache;
import com.parkai.backend.repository.CabaStreetPointRepository;
import com.parkai.backend.repository.StreetCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapService.class);

    private static final double SEARCH_RADIUS_METERS = 1000;
    private static final double FETCH_RADIUS_METERS = 1600;
    private static final double GRID_FACTOR = 200.0;
    private static final int MAX_RETURNED_STREETS = 100;
    private static final int MAX_CACHED_STREETS = 400;
    private static final int MAX_HTTP_BYTES = 8_000_000;
    private static final long FAILED_ZONE_COOLDOWN_MS = 120_000L;

    private static final List<String> OVERPASS_SERVERS = List.of(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.private.coffee/api/interpreter"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final StreetCacheRepository streetCacheRepository;
    private final CabaStreetPointRepository cabaStreetPointRepository;

    // Evita dos descargas simultaneas de la misma zona en esta instancia.
    private final Object[] zoneLocks = new Object[64];
    private final Map<String, Long> retryAfterByZone = new ConcurrentHashMap<>();

    public MapService(
            StreetCacheRepository streetCacheRepository,
            CabaStreetPointRepository cabaStreetPointRepository,
            ObjectMapper objectMapper
    ) {
        this.streetCacheRepository = streetCacheRepository;
        this.cabaStreetPointRepository = cabaStreetPointRepository;
        this.objectMapper = objectMapper;

        for (int i = 0; i < zoneLocks.length; i++) {
            zoneLocks[i] = new Object();
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(12000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public List<NearbyStreet> getNearbyStreets(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);

        // 1. Primera opcion: callejero oficial CABA, sin servicio externo.
        List<NearbyStreet> officialStreets = findOfficialCabaStreets(latitude, longitude);
        if (!officialStreets.isEmpty()) {
            LOGGER.info("MAP CABA LOCAL: {} calles cercanas", officialStreets.size());
            return officialStreets;
        }

        // 2. Segunda opcion: calles de otras zonas guardadas anteriormente.
        long latCell = Math.round(latitude * GRID_FACTOR);
        long lonCell = Math.round(longitude * GRID_FACTOR);
        String cacheKey = latCell + "_" + lonCell;
        List<NearbyStreet> cached = findCachedStreets(cacheKey, latitude, longitude);
        if (!cached.isEmpty()) {
            LOGGER.info("MAP CACHE HIT: zona={}, calles={}", cacheKey, cached.size());
            return cached;
        }

        Object lock = zoneLocks[Math.floorMod(cacheKey.hashCode(), zoneLocks.length)];
        synchronized (lock) {
            // Reconsultar caché: otra peticion pudo haberla cargado mientras esperabamos.
            cached = findCachedStreets(cacheKey, latitude, longitude);
            if (!cached.isEmpty()) {
                LOGGER.info("MAP CACHE HIT: zona={}, calles={}", cacheKey, cached.size());
                return cached;
            }

            long now = System.currentTimeMillis();
            if (now < retryAfterByZone.getOrDefault(cacheKey, 0L)) {
                LOGGER.info("MAP OVERPASS COOLDOWN: zona={}", cacheKey);
                return List.of();
            }

            LOGGER.info("MAP CACHE MISS: zona={}. Consultando Overpass...", cacheKey);
            double centerLat = latCell / GRID_FACTOR;
            double centerLon = lonCell / GRID_FACTOR;
            List<NearbyStreet> downloaded = requestOverpass(centerLat, centerLon);

            if (downloaded.isEmpty()) {
                retryAfterByZone.put(cacheKey, now + FAILED_ZONE_COOLDOWN_MS);
                LOGGER.warn("No hay calles disponibles para zona={}", cacheKey);
                return List.of();
            }

            retryAfterByZone.remove(cacheKey);
            List<StreetCache> rows = downloaded.stream()
                    .limit(MAX_CACHED_STREETS)
                    .map(street -> new StreetCache(
                            cacheKey,
                            street.streetName(),
                            street.latitude(),
                            street.longitude()
                    ))
                    .toList();

            try {
                streetCacheRepository.saveAll(rows);
                LOGGER.info("MAP CACHE SAVE: zona={}, calles={}", cacheKey, rows.size());
            } catch (Exception ex) {
                LOGGER.warn("No se pudo guardar la cache: {}", ex.getMessage());
            }

            return filterNearby(downloaded, latitude, longitude);
        }
    }

    private List<NearbyStreet> findOfficialCabaStreets(double lat, double lon) {
        double latDelta = SEARCH_RADIUS_METERS / 110_574.0;
        double lonDelta = SEARCH_RADIUS_METERS /
                (111_320.0 * Math.max(0.1, Math.abs(Math.cos(Math.toRadians(lat)))));

        List<CabaStreetPoint> rows = cabaStreetPointRepository
                .findByLatitudeBetweenAndLongitudeBetween(
                        lat - latDelta,
                        lat + latDelta,
                        lon - lonDelta,
                        lon + lonDelta
                );

        return filterNearby(
                rows.stream()
                        .map(point -> new NearbyStreet(
                                point.getStreetName(),
                                point.getLatitude(),
                                point.getLongitude()
                        ))
                        .toList(),
                lat,
                lon
        );
    }

    private List<NearbyStreet> findCachedStreets(
            String cacheKey,
            double latitude,
            double longitude
    ) {
        return filterNearby(
                streetCacheRepository.findByCacheKey(cacheKey).stream()
                        .map(row -> new NearbyStreet(
                                row.getStreetName(),
                                row.getLatitude(),
                                row.getLongitude()
                        ))
                        .toList(),
                latitude,
                longitude
        );
    }

    private List<NearbyStreet> requestOverpass(double lat, double lon) {
        String query = String.format(
                Locale.US,
                """
                [out:json][timeout:12];
                way(around:%.0f,%.7f,%.7f)["highway"]["name"];
                out center tags;
                """,
                FETCH_RADIUS_METERS, lat, lon
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        for (String server : OVERPASS_SERVERS) {
            try {
                LOGGER.info("Consultando Overpass: {}", server);
                Map<String, Object> response = restClient.post()
                        .uri(server)
                        // Para la API publica se recomienda una identificacion real
                        // y contacto del proyecto (correo o URL publica).
                        .header("User-Agent", "ParkAI/1.0 (UADE academic research project)")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(form)
                        .exchange((request, rawResponse) -> {
                            int status = rawResponse.getStatusCode().value();
                            byte[] bytes = rawResponse.getBody().readNBytes(MAX_HTTP_BYTES + 1);
                            if (bytes.length > MAX_HTTP_BYTES) {
                                throw new IOException("Overpass response too large");
                            }
                            String body = new String(bytes, StandardCharsets.UTF_8);
                            if (status < 200 || status >= 300) {
                                throw new IOException("HTTP " + status + ": " +
                                        body.substring(0, Math.min(160, body.length())));
                            }
                            if (!body.stripLeading().startsWith("{")) {
                                throw new IOException("Unexpected Overpass response: " +
                                        body.substring(0, Math.min(160, body.length())));
                            }
                            return objectMapper.readValue(
                                    body,
                                    new TypeReference<Map<String, Object>>() {}
                            );
                        });

                if (!(response.get("elements") instanceof List<?> elements)) {
                    LOGGER.warn("Overpass returned no elements: {}", server);
                    continue;
                }
                List<NearbyStreet> streets = parseStreets(elements);
                if (!streets.isEmpty()) {
                    LOGGER.info("Overpass OK: {} calles", streets.size());
                    return streets;
                }
            } catch (Exception ex) {
                LOGGER.warn("Overpass {}: {}", server, ex.getMessage());
            }
        }
        return List.of();
    }

    private List<NearbyStreet> parseStreets(List<?> elements) {
        List<NearbyStreet> streets = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Object item : elements) {
            if (!(item instanceof Map<?, ?> element) ||
                    !(element.get("tags") instanceof Map<?, ?> tags) ||
                    !(element.get("center") instanceof Map<?, ?> center)) {
                continue;
            }
            Object name = tags.get("name");
            Object lat = center.get("lat");
            Object lon = center.get("lon");
            if (name == null || lat == null || lon == null) {
                continue;
            }
            try {
                String streetName = name.toString().trim();
                double streetLat = Double.parseDouble(lat.toString());
                double streetLon = Double.parseDouble(lon.toString());
                if (streetName.isEmpty() || !validCoordinates(streetLat, streetLon)) {
                    continue;
                }
                String unique = streetName + "_" + streetLat + "_" + streetLon;
                if (seen.add(unique)) {
                    streets.add(new NearbyStreet(streetName, streetLat, streetLon));
                }
            } catch (NumberFormatException ignored) {
                // Ignorar coordenadas invalidas.
            }
        }
        return streets;
    }

    private List<NearbyStreet> filterNearby(
            List<NearbyStreet> streets,
            double latitude,
            double longitude
    ) {
        return streets.stream()
                .filter(s -> distanceMeters(
                        latitude, longitude, s.latitude(), s.longitude()
                ) <= SEARCH_RADIUS_METERS)
                .sorted(Comparator.comparingDouble(s -> distanceMeters(
                        latitude, longitude, s.latitude(), s.longitude()
                )))
                .limit(MAX_RETURNED_STREETS)
                .toList();
    }

    private boolean validCoordinates(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    private void validateCoordinates(double lat, double lon) {
        if (!validCoordinates(lat, lon)) {
            throw new IllegalArgumentException("Invalid geographic coordinates");
        }
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(deltaLon / 2), 2);
        return 2 * 6_371_000 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
