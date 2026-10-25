package com.parkai.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "parking_facilities", indexes = {
        @Index(name = "idx_parking_facilities_lat_lon", columnList = "latitude,longitude")
})
public class ParkingFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_key", nullable = false, unique = true, length = 80)
    private String sourceKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(length = 120)
    private String neighborhood;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public Long getId() { return id; }
    public String getSourceKey() { return sourceKey; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getNeighborhood() { return neighborhood; }
    public String getSource() { return source; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
