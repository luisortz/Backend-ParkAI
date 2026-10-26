package com.parkai.backend.dto;

import java.util.List;

public record StreetSegmentResponse(
        Long id,
        String streetName,
        double latitude,
        double longitude,
        List<SegmentCoordinate> coordinates
) {
}
