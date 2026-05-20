package com.parkai.backend.controller;

import com.parkai.backend.dto.FavoriteRequest;
import com.parkai.backend.dto.FavoriteResponse;
import com.parkai.backend.security.AuthenticatedUserProvider;
import com.parkai.backend.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    private final AuthenticatedUserProvider
            authenticatedUserProvider;

    public FavoriteController(
            FavoriteService favoriteService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.favoriteService = favoriteService;
        this.authenticatedUserProvider =
                authenticatedUserProvider;
    }

    @PostMapping
    public FavoriteResponse create(
            @RequestHeader("Authorization")
            String authorizationHeader,

            @RequestBody @Valid
            FavoriteRequest request
    ) {

        Long userId =
                authenticatedUserProvider
                        .getUserId(
                                authorizationHeader
                        );

        return favoriteService.create(
                userId,
                request
        );
    }

    @GetMapping
    public List<FavoriteResponse> getFavorites(

            @RequestHeader("Authorization")
            String authorizationHeader
    ) {

        Long userId =
                authenticatedUserProvider
                        .getUserId(
                                authorizationHeader
                        );

        return favoriteService
                .getUserFavorites(userId);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        favoriteService.delete(id);
    }
}