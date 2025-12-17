// package com.network.projet.ussd.repository;

// import org.springframework.data.r2dbc.repository.Query;
// import org.springframework.data.r2dbc.repository.R2dbcRepository;
// import org.springframework.stereotype.Repository;

// import com.network.projet.ussd.model.entities.Package;

// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;

// @Repository
// public interface PackageRepository extends R2dbcRepository<Package, Long> {
    
//     Mono<Package> findByPackageNumber(String packageNumber);
    
//     Flux<Package> findByShipmentId(Long shipmentId);
    
//     @Query("SELECT * FROM packages WHERE weight >= :minWeight AND weight <= :maxWeight")
//     Flux<Package> findByWeightRange(Double minWeight, Double maxWeight);
// }
