package com.parkai.backend.repository;

import com.parkai.backend.model.ParkingFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ParkingFacilityRepository extends JpaRepository<ParkingFacility, Long> {

    @Query("""
        SELECT p FROM ParkingFacility p
        WHERE p.latitude BETWEEN :minLat AND :maxLat
          AND p.longitude BETWEEN :minLon AND :maxLon
        """)
    List<ParkingFacility> findInBoundingBox(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon);
}
