// ============================================
// ShipmentRepository.java
// ============================================
package com.network.projet.ussd.repository;

import com.network.projet.ussd.model.entities.Shipment;
import com.network.projet.ussd.model.enums.ShipmentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repository for Shipment entity operations.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface ShipmentRepository extends R2dbcRepository<Shipment, Long> {
    
    /**
     * Finds shipment by tracking ID.
     * 
     * @param tracking_id tracking identifier
     * @return Mono with shipment or empty
     */
    Mono<Shipment> findByTrackingId(String tracking_id);
    
    /**
     * Finds all shipments for a sender.
     * 
     * @param sender_id sender ID
     * @return Flux of shipments
     */
    Flux<Shipment> findBySenderId(Long sender_id);
    
    /**
     * Finds shipments by status.
     * 
     * @param status shipment status
     * @return Flux of shipments
     */
    Flux<Shipment> findByStatus(ShipmentStatus status);
    
    /**
     * Finds recent shipments for a sender.
     * 
     * @param sender_id sender ID
     * @param limit maximum results
     * @return Flux of shipments
     */
    @Query("SELECT * FROM shipment " +
           "WHERE sender_id = :sender_id " +
           "ORDER BY created_at DESC " +
           "LIMIT :limit")
    Flux<Shipment> findRecentBySenderId(Long sender_id, Integer limit);
    
    /**
     * Counts shipments by sender and status.
     * 
     * @param sender_id sender ID
     * @param status shipment status
     * @return Mono with count
     */
    Mono<Long> countBySenderIdAndStatus(Long sender_id, ShipmentStatus status);
    
    /**
     * Finds shipments created within date range.
     * 
     * @param start_date start date
     * @param end_date end date
     * @return Flux of shipments
     */
    @Query("SELECT * FROM shipment " +
           "WHERE created_at BETWEEN :start_date AND :end_date " +
           "ORDER BY created_at DESC")
    Flux<Shipment> findByDateRange(LocalDateTime start_date, LocalDateTime end_date);
    
    /**
     * Gets next sequence number for tracking ID generation.
     * 
     * @return Mono with next sequence
     */
    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM shipment")
    Mono<Long> getNextSequence();
}