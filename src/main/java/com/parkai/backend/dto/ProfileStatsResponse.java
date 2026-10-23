package com.parkai.backend.dto;

public record ProfileStatsResponse(
        long favoritesCount,
        long reportsCount,
        long searchesCount
) {}