package com.parkai.backend.service;

import com.parkai.backend.dto.PrivateParkingResponse;
import com.parkai.backend.model.ParkingFacility;
import com.parkai.backend.repository.ParkingFacilityRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Service
public class PrivateParkingService {
    private static final double RADIUS_METERS = 2000.0;
    private static final int MAX_RESULTS = 40;
    private final ParkingFacilityRepository repository;

    public PrivateParkingService(ParkingFacilityRepository repository) {
        this.repository = repository;
    }

    public List<PrivateParkingResponse> findNearby(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas invalidas");
        }

        double latDelta = RADIUS_METERS / 111_320.0;
        double longitudeScale = Math.max(Math.abs(Math.cos(Math.toRadians(latitude))), 0.01);
        double lonDelta = RADIUS_METERS / (111_320.0 * longitudeScale);

        return repository.findInBoundingBox(
                    latitude - latDelta, latitude + latDelta,
                    longitude - lonDelta, longitude + lonDelta).stream()
                .filter(f -> distanceMeters(latitude, longitude,
                        f.getLatitude(), f.getLongitude()) <= RADIUS_METERS)
                .sorted(Comparator.comparingDouble(f -> distanceMeters(
                        latitude, longitude, f.getLatitude(), f.getLongitude())))
                .limit(MAX_RESULTS)
                .map(PrivateParkingResponse::from)
                .toList();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 6371000.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
