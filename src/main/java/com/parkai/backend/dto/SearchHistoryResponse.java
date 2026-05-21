package com.parkai.backend.dto;

import java.time.LocalDateTime;

public record SearchHistoryResponse(

        String placeName,

        Double latitude,

        Double longitude,

        LocalDateTime searchedAt

) {
}