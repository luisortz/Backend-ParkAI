package com.parkai.backend.service;

import com.parkai.backend.dto.SegmentCoordinate;
import com.parkai.backend.dto.StreetSegmentResponse;
import com.parkai.backend.model.CabaStreetPoint;
import com.parkai.backend.repository.CabaStreetPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CabaStreetSegmentService {

    // Coincide con el radio y limite del MapService actual.
    private static final double SEARCH_RADIUS_METERS = 1000;
    private static final int MAX_SEGMENTS = 150;

    private final CabaStreetPointRepository pointRepository;

    public CabaStreetSegmentService(CabaStreetPointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    @Transactional(readOnly = true)
    public List<StreetSegmentResponse> getNearbySegments(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid geographic coordinates");
        }

        // Primero consultamos los puntos centrales existentes. No modificamos
        // ni el MapService ni el servicio de ML ya funcionales.
        double latDelta = SEARCH_RADIUS_METERS / 110_574.0;
        double lonDelta = SEARCH_RADIUS_METERS /
                (111_320.0 * Math.max(0.1, Math.abs(Math.cos(Math.toRadians(latitude)))));

        List<CabaStreetPoint> nearby = pointRepository
                .findByLatitudeBetweenAndLongitudeBetween(
                        latitude - latDelta, latitude + latDelta,
                        longitude - lonDelta, longitude + lonDelta)
                .stream()
                .filter(point -> distanceMeters(latitude, longitude,
                        point.getLatitude(), point.getLongitude()) <= SEARCH_RADIUS_METERS)
                .sorted(Comparator.comparingDouble(point -> distanceMeters(
                        latitude, longitude, point.getLatitude(), point.getLongitude())))
                .limit(MAX_SEGMENTS)
                .toList();

        if (nearby.isEmpty()) {
            // No se cargan geometrías para otras ciudades: el frontend
            // mantiene los marcadores actuales de Overpass como fallback.
            return List.of();
        }

        // La geometría está en la MISMA entidad, sin joins ni tablas extra.
        List<StreetSegmentResponse> result = new ArrayList<>();
        for (CabaStreetPoint point : nearby) {
            List<SegmentCoordinate> coords = parseLineString(point.getGeometryWkt());
            if (coords.size() < 2) {
                continue;
            }
            result.add(new StreetSegmentResponse(
                    point.getSourceSegmentId(),
                    point.getStreetName(),
                    point.getLatitude(),
                    point.getLongitude(),
                    coords
            ));
        }
        return result;
    }

    // WKT guarda longitud ANTES de latitud; MapKit requiere lo contrario.
    private List<SegmentCoordinate> parseLineString(String wkt) {
        if (wkt == null) return List.of();
        String normalized = wkt.trim();
        int open = normalized.indexOf('(');
        if (!normalized.regionMatches(true, 0, "LINESTRING", 0, "LINESTRING".length())
                || open < 0 || !normalized.endsWith(")")) {
            return List.of();
        }
        String content = normalized.substring(open + 1, normalized.length() - 1);
        String[] points = content.split(",");
        List<SegmentCoordinate> coordinates = new ArrayList<>(points.length);

        try {
            for (String point : points) {
                String[] values = point.trim().split("\\s+");
                if (values.length != 2) return List.of();
                double lon = Double.parseDouble(values[0]);
                double lat = Double.parseDouble(values[1]);
                if (!Double.isFinite(lat) || !Double.isFinite(lon)
                        || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    return List.of();
                }
                coordinates.add(new SegmentCoordinate(lat, lon));
            }
        } catch (NumberFormatException ex) {
            return List.of();
        }
        return coordinates.size() >= 2 ? coordinates : List.of();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6_371_000 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
