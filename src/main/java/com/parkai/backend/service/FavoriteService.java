package com.parkai.backend.service;

import com.parkai.backend.dto.FavoriteRequest;
import com.parkai.backend.dto.FavoriteResponse;
import com.parkai.backend.exception.DuplicateFavoriteException;
import com.parkai.backend.model.Favorite;
import com.parkai.backend.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository
    ) {
        this.favoriteRepository = favoriteRepository;
    }

    public FavoriteResponse create(
        Long userId,
        FavoriteRequest request
) {

    String normalizedStreetName =
            request.streetName()
                    .trim()
                    .toLowerCase();

    boolean sameStreet =
            favoriteRepository
                    .findByUserId(userId)
                    .stream()
                    .anyMatch(favorite ->
                            favorite.getStreetName()
                                    .trim()
                                    .toLowerCase()
                                    .equals(normalizedStreetName)
                    );

    if (sameStreet) {

        throw new DuplicateFavoriteException(
        "You already have a favorite with this street name"
        );
    }

    boolean sameCoordinates =
            favoriteRepository
                    .existsByUserIdAndLatitudeAndLongitude(
                            userId,
                            request.latitude(),
                            request.longitude()
                    );

    if (sameCoordinates) {

        throw new DuplicateFavoriteException(
        "You already have a favorite for this location"
        );
    }

    Favorite favorite = new Favorite();

    favorite.setUserId(userId);

    favorite.setStreetName(
            request.streetName().trim()
    );

    favorite.setLatitude(
            request.latitude()
    );

    favorite.setLongitude(
            request.longitude()
    );

    Favorite saved =
            favoriteRepository.save(favorite);

    return new FavoriteResponse(
            saved.getId(),
            saved.getStreetName(),
            saved.getLatitude(),
            saved.getLongitude()
    );
}

    public List<FavoriteResponse> getUserFavorites(
            Long userId
    ) {

        return favoriteRepository
                .findByUserId(userId)
                .stream()
                .map(favorite -> new FavoriteResponse(
                        favorite.getId(),
                        favorite.getStreetName(),
                        favorite.getLatitude(),
                        favorite.getLongitude()
                ))
                .toList();
    }

    public void delete(Long favoriteId) {
        favoriteRepository.deleteById(favoriteId);
    }

    public List<Favorite> getFavorites(Long userId) {

        return favoriteRepository.findByUserId(userId);
    }

    public void deleteFavorite(
        Long favoriteId,
        Long userId
) {

    Favorite favorite = favoriteRepository
            .findByIdAndUserId(
                    favoriteId,
                    userId
            )
            .orElseThrow(() ->
                    new RuntimeException(
                            "Favorite not found"
                    )
            );

    favoriteRepository.delete(favorite);
}
}