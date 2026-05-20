package com.parkai.backend.repository;

import com.parkai.backend.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository
        extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByIdAndUserId(
            Long id,
            Long userId
    );

    boolean existsByUserIdAndStreetName(
        Long userId,
        String streetName
);

boolean existsByUserIdAndLatitudeAndLongitude(
        Long userId,
        Double latitude,
        Double longitude
);
}