package com.parkai.backend.repository;

import com.parkai.backend.model.CabaStreetPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CabaStreetPointRepository
        extends JpaRepository<CabaStreetPoint, Long> {

    List<CabaStreetPoint> findByLatitudeBetweenAndLongitudeBetween(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude
    );
}
