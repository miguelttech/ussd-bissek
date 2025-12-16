// ============================================
// SessionService.java
// ============================================
package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.SessionExpiredException;
import com.network.projet.ussd.repository.SessionRepository;
import com.network.projet.ussd.util.UssdConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing USSD session state.
 * 
 * <p>Session Management:
 * <ul>
 *   <li>Create new session on first USSD request</li>
 *   <li>Store user state between USSD interactions</li>
 *   <li>Expire sessions after timeout</li>
 *   <li>Clean up old sessions periodically</li>
 * </ul>
 * 
 * <p>Session Data Structure:
 * <pre>
 * {
 *   "user_id": 123,
 *   "current_state": "ENTER_WEIGHT",
 *   "phone": "+237600000000",
 *   "answers": {
 *     "recipient_name": "John Doe",
 *     "recipient_phone": "+237611111111"
 *   },
 *   "created_at": "2025-01-15T10:30:00",
 *   "last_activity": "2025-01-15T10:35:00"
 * }
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final SessionRepository session_repository;
    
    /**
     * Session timeout duration (from properties).
     */
    private static final Duration SESSION_TIMEOUT = 
        Duration.ofMinutes(UssdConstants.SESSION_TIMEOUT_MINUTES);
    
    /**
     * Creates a new USSD session.
     * 
     * @param phone_number user's phone number
     * @return Mono<String> session ID
     */
    public Mono<String> create_session(String phone_number) {
        String session_id = generate_session_id();
        
        log.info("Creating new session: {} for phone: {}", session_id, phone_number);
        
        Map<String, Object> session_data = new HashMap<>();
        session_data.put("phone", phone_number);
        session_data.put("current_state", "WELCOME");
        session_data.put("created_at", LocalDateTime.now().toString());
        session_data.put("last_activity", LocalDateTime.now().toString());
        session_data.put("answers", new HashMap<String, String>());
        
        return session_repository.save(session_id, session_data)
            .thenReturn(session_id);
    }
    
    /**
     * Gets session data.
     * 
     * @param session_id session identifier
     * @return Mono<Map> session data or error if expired
     */
    public Mono<Map<String, Object>> get_session(String session_id) {
        return session_repository.find(session_id)
            .flatMap(session_data -> {
                // Vérifier si la session a expiré
                String last_activity_str = (String) session_data.get("last_activity");
                LocalDateTime last_activity = LocalDateTime.parse(last_activity_str);
                
                if (is_expired(last_activity)) {
                    log.warn("Session expired: {}", session_id);
                    return session_repository.delete(session_id)
                        .then(Mono.error(new SessionExpiredException(
                            "Session has expired", session_id)));
                }
                
                // Mettre à jour last_activity
                session_data.put("last_activity", LocalDateTime.now().toString());
                return session_repository.save(session_id, session_data)
                    .thenReturn(session_data);
            })
            .switchIfEmpty(Mono.error(new SessionExpiredException(
                "Session not found", session_id)));
    }
    
    /**
     * Updates session data.
     * 
     * @param session_id session identifier
     * @param key data key
     * @param value data value
     * @return Mono<Void> completion signal
     */
    public Mono<Void> update_session(String session_id, String key, Object value) {
        return get_session(session_id)
            .flatMap(session_data -> {
                session_data.put(key, value);
                return session_repository.save(session_id, session_data);
            });
    }
    
    /**
     * Updates current state.
     * 
     * @param session_id session identifier
     * @param new_state new state ID
     * @return Mono<Void> completion signal
     */
    public Mono<Void> update_state(String session_id, String new_state) {
        log.debug("Updating session {} to state: {}", session_id, new_state);
        return update_session(session_id, "current_state", new_state);
    }
    
    /**
     * Stores user answer in session.
     * 
     * @param session_id session identifier
     * @param question_key question identifier
     * @param answer user's answer
     * @return Mono<Void> completion signal
     */
    public Mono<Void> store_answer(String session_id, String question_key, String answer) {
        return get_session(session_id)
            .flatMap(session_data -> {
                @SuppressWarnings("unchecked")
                Map<String, String> answers = (Map<String, String>) session_data.get("answers");
                answers.put(question_key, answer);
                return session_repository.save(session_id, session_data);
            });
    }
    
    /**
     * Gets stored answer.
     * 
     * @param session_id session identifier
     * @param question_key question identifier
     * @return Mono<String> answer or empty
     */
    public Mono<String> get_answer(String session_id, String question_key) {
        return get_session(session_id)
            .map(session_data -> {
                @SuppressWarnings("unchecked")
                Map<String, String> answers = (Map<String, String>) session_data.get("answers");
                return answers.get(question_key);
            });
    }
    
    /**
     * Gets all stored answers.
     * 
     * @param session_id session identifier
     * @return Mono<Map> all answers
     */
    public Mono<Map<String, String>> get_all_answers(String session_id) {
        return get_session(session_id)
            .map(session_data -> {
                @SuppressWarnings("unchecked")
                Map<String, String> answers = (Map<String, String>) session_data.get("answers");
                return answers;
            });
    }
    
    /**
     * Deletes session (on completion or error).
     * 
     * @param session_id session identifier
     * @return Mono<Void> completion signal
     */
    public Mono<Void> end_session(String session_id) {
        log.info("Ending session: {}", session_id);
        return session_repository.delete(session_id);
    }
    
    /**
     * Checks if session exists and is valid.
     * 
     * @param session_id session identifier
     * @return Mono<Boolean> true if valid
     */
    public Mono<Boolean> is_valid_session(String session_id) {
        return session_repository.exists(session_id);
    }
    
    /**
     * Generates unique session ID.
     * 
     * @return session ID
     */
    private String generate_session_id() {
        return UssdConstants.SESSION_ID_PREFIX + UUID.randomUUID().toString();
    }
    
    /**
     * Checks if session has expired.
     * 
     * @param last_activity last activity timestamp
     * @return true if expired
     */
    private boolean is_expired(LocalDateTime last_activity) {
        return Duration.between(last_activity, LocalDateTime.now())
            .compareTo(SESSION_TIMEOUT) > 0;
    }
}
