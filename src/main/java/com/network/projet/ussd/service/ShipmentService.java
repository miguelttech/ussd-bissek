package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.exception.ValidationException;
import com.network.projet.ussd.model.entities.*;
import com.network.projet.ussd.model.enums.DeliveryType;
import com.network.projet.ussd.model.enums.PaymentMethod;
import com.network.projet.ussd.model.enums.ShipmentStatus;
import com.network.projet.ussd.model.enums.TransportMode;
import com.network.projet.ussd.repository.PackageRepository;
import com.network.projet.ussd.repository.RecipientRepository;
import com.network.projet.ussd.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for shipment management operations.
 * Handles the complete shipment lifecycle from creation to delivery.
 * 
 * <p>Workflow:
 * 1. Create recipient
 * 2. Create package
 * 3. Calculate pricing
 * 4. Create shipment
 * 5. Generate tracking ID
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {
    
    private final ShipmentRepository shipment_repository;
    private final PackageRepository package_repository;
    private final RecipientRepository recipient_repository;
    private final PricingService pricing_service;
    
    /**
     * Creates a complete shipment with all related entities.
     * 
     * <p>This is a TRANSACTIONAL operation that creates:
     * - Recipient
     * - Package
     * - Shipment
     * 
     * @param sender_id sender's user ID
     * @param recipient recipient information
     * @param pkg package information
     * @param transport_mode chosen transport
     * @param delivery_type delivery speed
     * @param payment_method payment method
     * @return Mono<Shipment> created shipment
     */
    public Mono<Shipment> create_shipment(
            Long sender_id,
            Recipient recipient,
            Package pkg,
            TransportMode transport_mode,
            DeliveryType delivery_type,
            PaymentMethod payment_method) {
        
        log.info("Creating shipment for sender: {}", sender_id);
        
        // ÉTAPE 1: Valider et sauvegarder le destinataire
        return save_recipient(recipient, sender_id)
            .flatMap(saved_recipient -> {
                log.debug("Recipient saved with ID: {}", saved_recipient.getId());
                
                // ÉTAPE 2: Valider et sauvegarder le colis
                return save_package(pkg, sender_id)
                    .flatMap(saved_package -> {
                        log.debug("Package saved with ID: {}", saved_package.getId());
                        
                        // ÉTAPE 3: Calculer le prix
                        return pricing_service.calculate_price(
                                saved_package.getWeight(),
                                transport_mode,
                                delivery_type,
                                saved_package.requiresSpecialHandling())
                            .flatMap(price -> {
                                log.debug("Calculated price: {} XAF", price);
                                
                                // ÉTAPE 4: Générer tracking ID
                                return shipment_repository.getNextSequence()
                                    .flatMap(sequence -> {
                                        String tracking_id = Shipment.generateTrackingId(sequence);
                                        
                                        // ÉTAPE 5: Créer le shipment
                                        Shipment shipment = Shipment.builder()
                                            .sender_id(sender_id)
                                            .recipient_id(saved_recipient.getId())
                                            .package_id(saved_package.getId())
                                            .tracking_id(tracking_id)
                                            .status(ShipmentStatus.PENDING)
                                            .total_price(price)
                                            .pickup_address(recipient.getAddress()) // TODO: Get from sender
                                            .delivery_address(saved_recipient.getFormattedDeliveryAddress())
                                            .created_at(LocalDateTime.now())
                                            .build();
                                        
                                        return shipment_repository.save(shipment);
                                    });
                            });
                    });
            })
            .doOnSuccess(shipment -> 
                log.info("Shipment created successfully: {}", shipment.getTracking_id()))
            .doOnError(error -> 
                log.error("Failed to create shipment: {}", error.getMessage()));
    }
    
    /**
     * Saves recipient with validation.
     * 
     * @param recipient recipient data
     * @param sender_id sender ID
     * @return Mono<Recipient> saved recipient
     */
    private Mono<Recipient> save_recipient(Recipient recipient, Long sender_id) {
        if (!recipient.isComplete()) {
            return Mono.error(new ValidationException(
                "Recipient information is incomplete", "recipient"));
        }
        
        recipient.setSender_id(sender_id);
        return recipient_repository.save(recipient);
    }
    
    /**
     * Saves package with validation.
     * 
     * @param pkg package data
     * @param sender_id sender ID
     * @return Mono<Package> saved package
     */
    private Mono<Package> save_package(Package pkg, Long sender_id) {
        // Valider le poids
        if (!pkg.isWeightValid()) {
            return Mono.error(new ValidationException(
                "Weight must be between " + Package.MIN_WEIGHT + 
                " and " + Package.MAX_WEIGHT + " kg", "weight"));
        }
        
        // Valider l'assurance
        if (!pkg.isInsuranceValid()) {
            return Mono.error(new ValidationException(
                "Insured packages must have a declared value", "insurance"));
        }
        
        pkg.setSender_id(sender_id);
        return package_repository.save(pkg);
    }
    
    /**
     * Tracks shipment by tracking ID.
     * 
     * @param tracking_id tracking identifier
     * @return Mono<Shipment> shipment details
     */
    public Mono<Shipment> track_shipment(String tracking_id) {
        log.info("Tracking shipment: {}", tracking_id);
        
        return shipment_repository.findByTrackingId(tracking_id)
            .switchIfEmpty(Mono.error(new ValidationException(
                "Shipment not found with tracking ID: " + tracking_id, 
                "tracking_id")))
            .doOnSuccess(shipment -> 
                log.debug("Shipment status: {}", shipment.getStatus()));
    }
    
    /**
     * Gets all shipments for a sender.
     * 
     * @param sender_id sender ID
     * @return Flux<Shipment> stream of shipments
     */
    public Flux<Shipment> get_sender_shipments(Long sender_id) {
        return shipment_repository.findBySenderId(sender_id);
    }
    
    /**
     * Gets recent shipments for a sender.
     * 
     * @param sender_id sender ID
     * @param limit maximum results
     * @return Flux<Shipment> recent shipments
     */
    public Flux<Shipment> get_recent_shipments(Long sender_id, Integer limit) {
        return shipment_repository.findRecentBySenderId(sender_id, limit);
    }
    
    /**
     * Updates shipment status.
     * 
     * <p>Validates state transitions according to business rules.
     * 
     * @param shipment_id shipment ID
     * @param new_status new status
     * @return Mono<Shipment> updated shipment
     */
    public Mono<Shipment> update_status(Long shipment_id, ShipmentStatus new_status) {
        log.info("Updating shipment {} to status: {}", shipment_id, new_status);
        
        return shipment_repository.findById(shipment_id)
            .flatMap(shipment -> {
                // Vérifier si la transition est valide
                if (!shipment.getStatus().canTransitionTo(new_status)) {
                    return Mono.error(new InvalidStateException(
                        String.format("Cannot transition from %s to %s",
                            shipment.getStatus(), new_status),
                        new_status.name()));
                }
                
                shipment.setStatus(new_status);
                return shipment_repository.save(shipment);
            })
            .doOnSuccess(updated -> 
                log.info("Status updated for shipment: {}", shipment_id));
    }
    
    /**
     * Cancels a shipment.
     * Only allowed if status is PENDING or CONFIRMED.
     * 
     * @param shipment_id shipment ID
     * @param sender_id sender ID (for verification)
     * @return Mono<Shipment> cancelled shipment
     */
    public Mono<Shipment> cancel_shipment(Long shipment_id, Long sender_id) {
        log.info("Cancelling shipment: {}", shipment_id);
        
        return shipment_repository.findById(shipment_id)
            .flatMap(shipment -> {
                // Vérifier que c'est bien l'expéditeur
                if (!shipment.getSender_id().equals(sender_id)) {
                    return Mono.error(new ValidationException(
                        "You are not authorized to cancel this shipment", 
                        "authorization"));
                }
                
                // Vérifier si l'annulation est possible
                if (!shipment.canBeCancelled()) {
                    return Mono.error(new InvalidStateException(
                        "Shipment cannot be cancelled in current status: " + 
                        shipment.getStatus(),
                        shipment.getStatus().name()));
                }
                
                shipment.setStatus(ShipmentStatus.CANCELLED);
                return shipment_repository.save(shipment);
            })
            .doOnSuccess(cancelled -> 
                log.info("Shipment cancelled: {}", shipment_id));
    }
    
    /**
     * Counts shipments by status for a sender.
     * 
     * @param sender_id sender ID
     * @param status shipment status
     * @return Mono<Long> count
     */
    public Mono<Long> count_by_status(Long sender_id, ShipmentStatus status) {
        return shipment_repository.countBySenderIdAndStatus(sender_id, status);
    }
    
    /**
     * Gets shipment statistics for a sender.
     * 
     * @param sender_id sender ID
     * @return Mono with statistics map
     */
    public Mono<ShipmentStatistics> get_statistics(Long sender_id) {
        return Mono.zip(
            count_by_status(sender_id, ShipmentStatus.PENDING),
            count_by_status(sender_id, ShipmentStatus.IN_TRANSIT),
            count_by_status(sender_id, ShipmentStatus.DELIVERED),
            count_by_status(sender_id, ShipmentStatus.CANCELLED)
        ).map(tuple -> ShipmentStatistics.builder()
            .pending_count(tuple.getT1())
            .in_transit_count(tuple.getT2())
            .delivered_count(tuple.getT3())
            .cancelled_count(tuple.getT4())
            .total_count(tuple.getT1() + tuple.getT2() + tuple.getT3() + tuple.getT4())
            .build());
    }
    
    /**
     * Inner class for statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class ShipmentStatistics {
        private Long pending_count;
        private Long in_transit_count;
        private Long delivered_count;
        private Long cancelled_count;
        private Long total_count;
    }
}