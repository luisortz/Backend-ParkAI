package com.parkai.backend.service;

import com.parkai.backend.dto.SearchHistoryResponse;
import com.parkai.backend.model.SearchHistory;
import com.parkai.backend.repository.SearchHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SearchHistoryService {

    private final SearchHistoryRepository repository;

    public SearchHistoryService(
            SearchHistoryRepository repository
    ) {
        this.repository = repository;
    }

    public void saveSearch(
            Long userId,
            String placeName,
            Double latitude,
            Double longitude
    ) {

        Optional<SearchHistory> lastSearch =
                repository
                        .findTopByUserIdOrderBySearchedAtDesc(
                                userId
                        );

        if (lastSearch.isPresent()) {

            SearchHistory last =
                    lastSearch.get();

            boolean samePlace =
                    last.getPlaceName()
                            .trim()
                            .equalsIgnoreCase(
                                    placeName.trim()
                            );

            if (samePlace) {
                return;
            }
        }

        SearchHistory search =
                new SearchHistory();

        search.setUserId(userId);

        search.setPlaceName(placeName);

        search.setLatitude(latitude);

        search.setLongitude(longitude);

        search.setSearchedAt(
                LocalDateTime.now()
        );

        repository.save(search);

        cleanupOldSearches(userId);
    }

    private void cleanupOldSearches(
            Long userId
    ) {

        List<SearchHistory> searches =
                repository
                        .findTop10ByUserIdOrderBySearchedAtDesc(
                                userId
                        );

        List<Long> idsToKeep =
                searches.stream()
                        .map(SearchHistory::getId)
                        .toList();

        List<SearchHistory> toDelete =
                repository.findAll()
                        .stream()
                        .filter(search ->
                                search.getUserId()
                                        .equals(userId)
                                        &&
                                        !idsToKeep.contains(
                                                search.getId()
                                        )
                        )
                        .toList();

        repository.deleteAll(toDelete);
    }

    public List<SearchHistoryResponse>
    getHistory(Long userId) {

        return repository
                .findTop10ByUserIdOrderBySearchedAtDesc(
                        userId
                )
                .stream()
                .map(search ->
                        new SearchHistoryResponse(
                                search.getPlaceName(),
                                search.getLatitude(),
                                search.getLongitude(),
                                search.getSearchedAt()
                        )
                )
                .toList();
    }
}