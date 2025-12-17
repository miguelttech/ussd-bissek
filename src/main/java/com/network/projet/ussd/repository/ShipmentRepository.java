// package com.network.projet.ussd.repository;

// import org.springframework.data.r2dbc.repository.Query;
// import org.springframework.data.r2dbc.repository.R2dbcRepository;
// import org.springframework.stereotype.Repository;

// import com.network.projet.ussd.model.entities.Shipment;

// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;

// @Repository
// public interface ShipmentRepository extends R2dbcRepository<Shipment, Long> {
    
//     Mono<Shipment> findByTrackingNumber(String trackingNumber);
    
//     Flux<Shipment> findByUserId(Long senderId);
    
//     @Query("SELECT * FROM shipments WHERE status = :status")
//     Flux<Shipment> findByStatus(String status);
    
//     @Query("SELECT * FROM shipments WHERE created_at >= :date")
//     Flux<Shipment> findRecentShipments(String date);
// }
