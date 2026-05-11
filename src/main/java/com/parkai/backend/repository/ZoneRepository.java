package com.parkai.backend.repository;

import com.parkai.backend.model.Zone;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    List<Zone> findByLatitudeBetweenAndLongitudeBetween(
            double minLat,
            double maxLat,
            double minLng,
            double maxLng
    );
}
