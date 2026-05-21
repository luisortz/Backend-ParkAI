package com.parkai.backend.controller;

import com.parkai.backend.dto.SearchHistoryResponse;
import com.parkai.backend.security.AuthenticatedUserProvider;
import com.parkai.backend.service.SearchHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {

    private final SearchHistoryService
            searchHistoryService;

    private final AuthenticatedUserProvider
            authenticatedUserProvider;

    public SearchHistoryController(
            SearchHistoryService searchHistoryService,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {

        this.searchHistoryService =
                searchHistoryService;

        this.authenticatedUserProvider =
                authenticatedUserProvider;
    }

    @GetMapping
    public List<SearchHistoryResponse>
    getHistory(

            @RequestHeader("Authorization")
            String authorizationHeader
    ) {

        Long userId =
                authenticatedUserProvider
                        .getUserId(
                                authorizationHeader
                        );

        return searchHistoryService
                .getHistory(userId);
    }
}