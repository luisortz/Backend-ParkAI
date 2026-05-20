package com.parkai.backend.service;

import com.parkai.backend.dto.FavoriteRequest;
import com.parkai.backend.dto.FavoriteResponse;
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

        Favorite favorite = new Favorite();

        favorite.setUserId(userId);

        favorite.setStreetName(
                request.streetName()
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
}