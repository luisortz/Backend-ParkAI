
package com.parkai.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "street_cache",
    indexes = {
        @Index(
            name = "idx_street_cache_key",
            columnList = "cache_key"
        )
    }
)
public class StreetCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, length = 80)
    private String cacheKey;

    @Column(name = "street_name", nullable = false)
    private String streetName;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    public StreetCache() {
    }

    public StreetCache(
            String cacheKey,
            String streetName,
            double latitude,
            double longitude
    ) {
        this.cacheKey = cacheKey;
        this.streetName = streetName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cachedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCacheKey() {
        return cacheKey;
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

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }
}
