
package com.parkai.backend.service;

import com.parkai.backend.dto.ProfileStatsResponse;
import com.parkai.backend.repository.FavoriteRepository;
import com.parkai.backend.repository.ParkingReportRepository;
import com.parkai.backend.repository.SearchHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileStatsService {

    private final FavoriteRepository favoriteRepository;
    private final ParkingReportRepository parkingReportRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    public ProfileStatsService(
            FavoriteRepository favoriteRepository,
            ParkingReportRepository parkingReportRepository,
            SearchHistoryRepository searchHistoryRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.parkingReportRepository = parkingReportRepository;
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ProfileStatsResponse getStats(Long userId) {

        long favorites = favoriteRepository.countByUserId(userId);

        long reports = parkingReportRepository.countByUserId(userId);

        long searches = searchHistoryRepository.countByUserId(userId);

        return new ProfileStatsResponse(
                favorites,
                reports,
                searches
        );
    }
}
