package com.network.projet.ussd.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import com.network.projet.ussd.model.entities.Session;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SessionRepository extends R2dbcRepository<Session, Long> {
    
    Mono<Session> findBySessionId(String sessionId);
    
    Mono<Session> findByUserId(Long userId);
    
    @Query("SELECT * FROM sessions WHERE user_id = :userId AND is_active = true")
    Flux<Session> findActiveSessionsByUserId(Long userId);
    
    @Query("DELETE FROM sessions WHERE is_active = false")
    Mono<Void> deleteInactiveSessions();
}
