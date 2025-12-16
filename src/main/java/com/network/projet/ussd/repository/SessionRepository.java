// ============================================
// SessionRepository.java
// ============================================
package com.network.projet.ussd.repository;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session storage for USSD sessions.
 * Will be replaced with Redis in production.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public class SessionRepository {
    
    private final Map<String, Map<String, Object>> session_store = new ConcurrentHashMap<>();
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(10);
    
    /**
     * Saves session data.
     * 
     * @param session_id session identifier
     * @param data session data map
     * @return Mono with completion
     */
    public Mono<Void> save(String session_id, Map<String, Object> data) {
        return Mono.fromRunnable(() -> session_store.put(session_id, data));
    }
    
    /**
     * Retrieves session data.
     * 
     * @param session_id session identifier
     * @return Mono with session data or empty
     */
    public Mono<Map<String, Object>> find(String session_id) {
        return Mono.justOrEmpty(session_store.get(session_id));
    }
    
    /**
     * Deletes session.
     * 
     * @param session_id session identifier
     * @return Mono with completion
     */
    public Mono<Void> delete(String session_id) {
        return Mono.fromRunnable(() -> session_store.remove(session_id));
    }
    
    /**
     * Checks if session exists.
     * 
     * @param session_id session identifier
     * @return Mono with true if exists
     */
    public Mono<Boolean> exists(String session_id) {
        return Mono.just(session_store.containsKey(session_id));
    }
}
