// ============================================
// SessionService.java
// Enhanced session management with SessionContext model
// ============================================
package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.SessionExpiredException;
import com.network.projet.ussd.model.SessionContext;
import com.network.projet.ussd.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing USSD session state with enhanced context model.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Create and initialize sessions</li>
 *   <li>Store and retrieve session context</li>
 *   <li>Manage session lifecycle</li>
 *   <li>Handle session expiration</li>
 *   <li>Provide session statistics</li>
 * </ul>
 * 
 * <p>Session storage:
 * Current implementation uses in-memory storage (SessionRepository).
 * Production should use Redis for distributed sessions and persistence.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final SessionRepository sessionRepository;
    
    /**
     * Session timeout in seconds.
     * Configurable via application.properties: app.session.timeout
     * Default: 600 seconds (10 minutes)
     */
    @Value("${app.session.timeout:600}")
    private long sessionTimeoutSeconds;
    
    /**
     * Session ID prefix for identification.
     */
    private static final String SESSION_ID_PREFIX = "USSD_";
    
    /**
     * Creates a new USSD session.
     * 
     * @param phoneNumber user's phone number
     * @return Mono with session ID
     */
    public Mono<String> createSession(String phoneNumber) {
        String sessionId = generateSessionId();
        
        log.info("Creating new session: {} for phone: {}", sessionId, phoneNumber);
        
        SessionContext context = SessionContext.builder()
            .sessionId(sessionId)
            .phoneNumber(phoneNumber)
            .currentStateId(null) // Will be set by UssdService
            .authenticated(false)
            .build();
        
        return sessionRepository.save(sessionId, context.toMap())
            .thenReturn(sessionId)
            .doOnSuccess(id -> log.debug("Session created: {}", id));
    }
    
    /**
     * Gets session context.
     * Validates expiration and updates activity timestamp.
     * 
     * @param sessionId session identifier
     * @return Mono with session context
     */
    public Mono<SessionContext> getSessionContext(String sessionId) {
        return sessionRepository.find(sessionId)
            .flatMap(sessionMap -> {
                SessionContext context = SessionContext.fromMap(sessionMap);
                
                // Check if expired
                if (context.isExpired(sessionTimeoutSeconds)) {
                    log.warn("Session expired: {}", sessionId);
                    return sessionRepository.delete(sessionId)
                        .then(Mono.error(new SessionExpiredException(
                            "Session has expired", sessionId)));
                }
                
                // Update activity
                context.updateActivity();
                
                // Save updated timestamp
                return sessionRepository.save(sessionId, context.toMap())
                    .thenReturn(context);
            })
            .switchIfEmpty(Mono.error(new SessionExpiredException(
                "Session not found", sessionId)));
    }
    
    /**
     * Gets session data as map (legacy compatibility).
     * 
     * @param sessionId session identifier
     * @return Mono with session map
     */
    public Mono<Map<String, Object>> getSession(String sessionId) {
        return getSessionContext(sessionId)
            .map(SessionContext::toMap);
    }
    
    /**
     * Updates session context.
     * 
     * @param sessionId session identifier
     * @param key data key
     * @param value data value
     * @return Mono completion
     */
    public Mono<Void> updateSession(String sessionId, String key, Object value) {
        return getSessionContext(sessionId)
            .flatMap(context -> {
                // Update based on key type
                switch (key) {
                    case "currentStateId" -> context.setCurrentStateId((String) value);
                    case "userId" -> context.setUserId(value != null ? ((Number) value).longValue() : null);
                    case "authenticated" -> context.setAuthenticated((Boolean) value);
                    case "language" -> context.setLanguage((String) value);
                    default -> context.storeMetadata(key, value);
                }
                
                return sessionRepository.save(sessionId, context.toMap());
            });
    }
    
    /**
     * Updates current state in session.
     * 
     * @param sessionId session identifier
     * @param newStateId new state ID
     * @return Mono completion
     */
    public Mono<Void> updateState(String sessionId, String newStateId) {
        log.debug("Updating session {} to state: {}", sessionId, newStateId);
        return updateSession(sessionId, "currentStateId", newStateId);
    }
    
    /**
     * Stores user answer in session.
     * 
     * @param sessionId session identifier
     * @param questionKey question identifier
     * @param answer user's answer
     * @return Mono completion
     */
    public Mono<Void> storeAnswer(String sessionId, String questionKey, String answer) {
        return getSessionContext(sessionId)
            .flatMap(context -> {
                context.storeAnswer(questionKey, answer);
                context.resetRetry(); // Reset retry count on successful input
                return sessionRepository.save(sessionId, context.toMap());
            });
    }
    
    /**
     * Gets stored answer.
     * 
     * @param sessionId session identifier
     * @param questionKey question identifier
     * @return Mono with answer or empty
     */
    public Mono<String> getAnswer(String sessionId, String questionKey) {
        return getSessionContext(sessionId)
            .map(context -> context.getAnswer(questionKey))
            .filter(answer -> answer != null);
    }
    
    /**
     * Gets all stored answers.
     * 
     * @param sessionId session identifier
     * @return Mono with answers map
     */
    public Mono<Map<String, String>> getAllAnswers(String sessionId) {
        return getSessionContext(sessionId)
            .map(SessionContext::getAnswers);
    }
    
    /**
     * Increments retry count for current state.
     * Used when validation fails.
     * 
     * @param sessionId session identifier
     * @return Mono with updated retry count
     */
    public Mono<Integer> incrementRetry(String sessionId) {
        return getSessionContext(sessionId)
            .flatMap(context -> {
                context.incrementRetry();
                return sessionRepository.save(sessionId, context.toMap())
                    .thenReturn(context.getRetryCount());
            });
    }
    
    /**
     * Resets retry count.
     * 
     * @param sessionId session identifier
     * @return Mono completion
     */
    public Mono<Void> resetRetry(String sessionId) {
        return getSessionContext(sessionId)
            .flatMap(context -> {
                context.resetRetry();
                return sessionRepository.save(sessionId, context.toMap());
            });
    }
    
    /**
     * Checks if session has exceeded max retries.
     * 
     * @param sessionId session identifier
     * @param maxRetries maximum allowed retries
     * @return Mono with true if exceeded
     */
    public Mono<Boolean> hasExceededRetries(String sessionId, int maxRetries) {
        return getSessionContext(sessionId)
            .map(context -> context.hasExceededRetries(maxRetries));
    }
    
    /**
     * Ends session and clears data.
     * 
     * @param sessionId session identifier
     * @return Mono completion
     */
    public Mono<Void> endSession(String sessionId) {
        log.info("Ending session: {}", sessionId);
        return sessionRepository.delete(sessionId)
            .doOnSuccess(v -> log.debug("Session ended: {}", sessionId));
    }
    
    /**
     * Checks if session exists and is valid.
     * 
     * @param sessionId session identifier
     * @return Mono with true if valid
     */
    public Mono<Boolean> isValidSession(String sessionId) {
        return sessionRepository.exists(sessionId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.just(false);
                }
                
                // Check if expired
                return getSessionContext(sessionId)
                    .map(context -> !context.isExpired(sessionTimeoutSeconds))
                    .onErrorReturn(false);
            });
    }
    
    /**
     * Authenticates session with user ID.
     * 
     * @param sessionId session identifier
     * @param userId user ID
     * @return Mono completion
     */
    public Mono<Void> authenticateSession(String sessionId, Long userId) {
        return getSessionContext(sessionId)
            .flatMap(context -> {
                context.setUserId(userId);
                context.setAuthenticated(true);
                return sessionRepository.save(sessionId, context.toMap());
            })
            .doOnSuccess(v -> log.info("Session authenticated: {} for user: {}", sessionId, userId));
    }
    
    /**
     * Gets session statistics.
     * 
     * @param sessionId session identifier
     * @return Mono with statistics map
     */
    public Mono<Map<String, Object>> getSessionStatistics(String sessionId) {
        return getSessionContext(sessionId)
            .map(context -> {
                Map<String, Object> stats = Map.of(
                    "sessionId", context.getSessionId(),
                    "phoneNumber", context.getPhoneNumber(),
                    "currentState", context.getCurrentStateId(),
                    "authenticated", context.isAuthenticated(),
                    "durationSeconds", context.getSessionDurationSeconds(),
                    "idleSeconds", context.getIdleTimeSeconds(),
                    "answersCount", context.getAnswers().size(),
                    "retryCount", context.getRetryCount()
                );
                return stats;
            });
    }
    
    /**
     * Generates unique session ID.
     * 
     * @return session ID
     */
    private String generateSessionId() {
        return SESSION_ID_PREFIX + UUID.randomUUID().toString();
    }
    
    /**
     * Cleans up expired sessions.
     * Should be called periodically (scheduled task).
     * 
     * @return Mono with number of sessions cleaned
     */
    public Mono<Long> cleanupExpiredSessions() {
        log.info("Starting cleanup of expired sessions...");
        // Implementation depends on SessionRepository capabilities
        // For now, just log
        return Mono.just(0L)
            .doOnSuccess(count -> log.info("Cleaned up {} expired sessions", count));
    }
}