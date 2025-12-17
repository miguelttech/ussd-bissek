// ============================================
// SessionContext.java
// Model for USSD session context data
// ============================================
package com.network.projet.ussd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the complete context of a USSD session.
 * Encapsulates all data needed to maintain user state across USSD interactions.
 * 
 * <p>Session lifecycle:
 * <pre>
 * 1. User dials USSD code
 * 2. SessionContext created
 * 3. User navigates through states
 * 4. Answers stored in context
 * 5. Session ends (success/error/timeout)
 * 6. SessionContext cleared
 * </pre>
 * 
 * <p>Data stored:
 * <ul>
 *   <li>Session identification (ID, phone number)</li>
 *   <li>Current state in automaton</li>
 *   <li>User answers collected so far</li>
 *   <li>User authentication data</li>
 *   <li>Timestamps for timeout management</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {
    
    /**
     * Unique session identifier.
     * Provided by Africa's Talking or generated.
     */
    private String sessionId;
    
    /**
     * User's phone number in E.164 format.
     * Example: +237600000000
     */
    private String phoneNumber;
    
    /**
     * Current state ID in automaton.
     * Example: "STATE_9", "STATE_CONFIRM"
     */
    private String currentStateId;
    
    /**
     * Authenticated user ID (if user is logged in).
     * Null for unauthenticated sessions.
     */
    private Long userId;
    
    /**
     * User answers collected during session.
     * Key: question identifier (e.g., "recipientName")
     * Value: user's answer
     */
    @Builder.Default
    private Map<String, String> answers = new HashMap<>();
    
    /**
     * Additional metadata for business logic.
     * Can store calculated values, temporary data, etc.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Session creation timestamp.
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * Last activity timestamp.
     * Updated on every interaction.
     */
    @Builder.Default
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    /**
     * Number of retry attempts on current state.
     * Used for validation error handling.
     */
    @Builder.Default
    private int retryCount = 0;
    
    /**
     * Whether session is authenticated.
     */
    @Builder.Default
    private boolean authenticated = false;
    
    /**
     * Language preference (for future i18n support).
     * Default: "en"
     */
    @Builder.Default
    private String language = "en";
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Stores an answer in session context.
     * 
     * @param key answer key
     * @param value answer value
     */
    public void storeAnswer(String key, String value) {
        if (answers == null) {
            answers = new HashMap<>();
        }
        answers.put(key, value);
        updateActivity();
    }
    
    /**
     * Retrieves an answer from session context.
     * 
     * @param key answer key
     * @return answer value or null
     */
    public String getAnswer(String key) {
        return answers != null ? answers.get(key) : null;
    }
    
    /**
     * Checks if an answer exists.
     * 
     * @param key answer key
     * @return true if answer exists
     */
    public boolean hasAnswer(String key) {
        return answers != null && answers.containsKey(key);
    }
    
    /**
     * Stores metadata.
     * 
     * @param key metadata key
     * @param value metadata value
     */
    public void storeMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * Retrieves metadata.
     * 
     * @param key metadata key
     * @return metadata value or null
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * Updates last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * Increments retry count.
     */
    public void incrementRetry() {
        this.retryCount++;
    }
    
    /**
     * Resets retry count.
     */
    public void resetRetry() {
        this.retryCount = 0;
    }
    
    /**
     * Checks if session has exceeded max retries.
     * 
     * @param maxRetries maximum allowed retries
     * @return true if exceeded
     */
    public boolean hasExceededRetries(int maxRetries) {
        return retryCount >= maxRetries;
    }
    
    /**
     * Converts to map for storage.
     * Used when saving to Redis or other storage.
     * 
     * @return map representation
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", sessionId);
        map.put("phoneNumber", phoneNumber);
        map.put("currentStateId", currentStateId);
        map.put("userId", userId);
        map.put("answers", answers);
        map.put("metadata", metadata);
        map.put("createdAt", createdAt.toString());
        map.put("lastActivityAt", lastActivityAt.toString());
        map.put("retryCount", retryCount);
        map.put("authenticated", authenticated);
        map.put("language", language);
        return map;
    }
    
    /**
     * Creates SessionContext from map.
     * Used when loading from Redis or other storage.
     * 
     * @param map map representation
     * @return SessionContext instance
     */
    @SuppressWarnings("unchecked")
    public static SessionContext fromMap(Map<String, Object> map) {
        return SessionContext.builder()
            .sessionId((String) map.get("sessionId"))
            .phoneNumber((String) map.get("phoneNumber"))
            .currentStateId((String) map.get("currentStateId"))
            .userId(map.get("userId") != null ? 
                ((Number) map.get("userId")).longValue() : null)
            .answers((Map<String, String>) map.getOrDefault("answers", new HashMap<>()))
            .metadata((Map<String, Object>) map.getOrDefault("metadata", new HashMap<>()))
            .createdAt(map.get("createdAt") != null ? 
                LocalDateTime.parse((String) map.get("createdAt")) : LocalDateTime.now())
            .lastActivityAt(map.get("lastActivityAt") != null ? 
                LocalDateTime.parse((String) map.get("lastActivityAt")) : LocalDateTime.now())
            .retryCount(map.get("retryCount") != null ? 
                ((Number) map.get("retryCount")).intValue() : 0)
            .authenticated(map.get("authenticated") != null ? 
                (Boolean) map.get("authenticated") : false)
            .language((String) map.getOrDefault("language", "en"))
            .build();
    }
    
    /**
     * Gets session duration in seconds.
     * 
     * @return duration in seconds
     */
    public long getSessionDurationSeconds() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).getSeconds();
    }
    
    /**
     * Gets idle time in seconds.
     * 
     * @return idle time in seconds
     */
    public long getIdleTimeSeconds() {
        return java.time.Duration.between(lastActivityAt, LocalDateTime.now()).getSeconds();
    }
    
    /**
     * Checks if session is expired based on idle time.
     * 
     * @param maxIdleSeconds maximum idle time in seconds
     * @return true if expired
     */
    public boolean isExpired(long maxIdleSeconds) {
        return getIdleTimeSeconds() > maxIdleSeconds;
    }
    
    @Override
    public String toString() {
        return String.format("SessionContext{id='%s', phone='%s', state='%s', authenticated=%b, answers=%d}", 
            sessionId, phoneNumber, currentStateId, authenticated, 
            answers != null ? answers.size() : 0);
    }
}