// ============================================
// RelayPointRepository.java
// ============================================
package com.network.projet.ussd.repository;

import com.network.projet.ussd.model.entities.RelayPoint;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for RelayPoint entity operations.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface RelayPointRepository extends R2dbcRepository<RelayPoint, Long> {
    
    /**
     * Finds active relay points by city.
     * 
     * @param city city name
     * @return Flux of relay points
     */
    Flux<RelayPoint> findByCityAndIsActiveTrue(String city);
    
    /**
     * Finds all active relay points.
     * 
     * @return Flux of relay points
     */
    Flux<RelayPoint> findByIsActiveTrue();
}