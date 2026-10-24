
package com.parkai.backend.repository;

import com.parkai.backend.model.StreetCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreetCacheRepository
        extends JpaRepository<StreetCache, Long> {

    List<StreetCache> findByCacheKey(String cacheKey);
}
