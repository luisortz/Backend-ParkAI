package com.parkai.backend.dto;

import com.parkai.backend.model.ParkingFacility;

public record PrivateParkingResponse(
        Long id,
        String name,
        String address,
        String neighborhood,
        String source,
        double latitude,
        double longitude
) {
    public static PrivateParkingResponse from(ParkingFacility facility) {
        return new PrivateParkingResponse(
                facility.getId(),
                facility.getName(),
                facility.getAddress(),
                facility.getNeighborhood(),
                facility.getSource(),
                facility.getLatitude(),
                facility.getLongitude());
    }
}
