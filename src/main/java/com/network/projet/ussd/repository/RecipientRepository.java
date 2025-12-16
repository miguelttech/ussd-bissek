package com.network.projet.ussd.repository;

import com.network.projet.ussd.model.entities.Recipient;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for Recipient entity operations.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface RecipientRepository extends R2dbcRepository<Recipient, Long> {
    
    /**
     * Finds recipients by sender.
     * 
     * @param sender_id sender ID
     * @return Flux of recipients
     */
    Flux<Recipient> findBySenderId(Long sender_id);
    
    /**
     * Finds recipient by phone and sender.
     * 
     * @param phone phone number
     * @param sender_id sender ID
     * @return Mono with recipient or empty
     */
    Mono<Recipient> findByPhoneAndSenderId(String phone, Long sender_id);
    
    /**
     * Finds recipients by city.
     * 
     * @param city city name
     * @return Flux of recipients
     */
    Flux<Recipient> findByCity(String city);
}
