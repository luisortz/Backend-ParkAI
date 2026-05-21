package com.parkai.backend.repository;

import com.parkai.backend.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository
        extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory>
    findTop10ByUserIdOrderBySearchedAtDesc(
            Long userId
    );

    Optional<SearchHistory>
    findTopByUserIdOrderBySearchedAtDesc(
            Long userId
    );

    long countByUserId(Long userId);
}