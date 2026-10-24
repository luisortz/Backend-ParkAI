package com.parkai.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "caba_street_points",
        indexes = @Index(
                name = "idx_caba_street_geo",
                columnList = "latitude,longitude"
        )
)
public class CabaStreetPoint {

    @Id
    @Column(name = "source_segment_id")
    private Long sourceSegmentId;

    @Column(name = "street_name", nullable = false, length = 255)
    private String streetName;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "road_type", nullable = false, length = 60)
    private String roadType;

    @Column(name = "neighborhood", length = 120)
    private String neighborhood;

    protected CabaStreetPoint() {
        // Constructor requerido por JPA.
    }

    public Long getSourceSegmentId() {
        return sourceSegmentId;
    }

    public String getStreetName() {
        return streetName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getRoadType() {
        return roadType;
    }

    public String getNeighborhood() {
        return neighborhood;
    }
}
