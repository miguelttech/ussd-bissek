// ============================================
// PackageRepository.java
// ============================================
package com.network.projet.ussd.repository;

import com.network.projet.ussd.model.entities.Package;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for Package entity operations.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface PackageRepository extends R2dbcRepository<Package, Long> {
    
    /**
     * Finds packages by sender.
     * 
     * @param sender_id sender ID
     * @return Flux of packages
     */
    Flux<Package> findBySenderId(Long sender_id);
    
    /**
     * Finds packages requiring special handling.
     * 
     * @return Flux of packages
     */
    @Query("SELECT * FROM package " +
           "WHERE fragile = true OR perishable = true OR liquid = true")
    Flux<Package> findRequiringSpecialHandling();
    
    /**
     * Finds insured packages.
     * 
     * @return Flux of packages
     */
    Flux<Package> findByInsuredTrue();
}
