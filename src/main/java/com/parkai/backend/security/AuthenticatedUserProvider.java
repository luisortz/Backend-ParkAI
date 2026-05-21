package com.parkai.backend.security;

import com.parkai.backend.service.JwtService;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final JwtService jwtService;

    public AuthenticatedUserProvider(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    public Long getUserId(
            String authorizationHeader
    ) {

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new RuntimeException(
                    "Missing token"
            );
        }

        String token =
                authorizationHeader.replace(
                        "Bearer ",
                        ""
                );

        return jwtService.extractUserId(token);
    }
}