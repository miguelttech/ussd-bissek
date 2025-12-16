package com.network.projet.ussd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security configuration for the application.
 * Provides beans for password encryption and authentication.
 * 
 * <p>BCrypt is a one-way hashing algorithm that:
 * <ul>
 *   <li>Cannot be reversed (secure)</li>
 *   <li>Adds random salt automatically</li>
 *   <li>Slow by design (prevents brute force)</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>
 * Plain: "MyPassword123"
 * Hash:  "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Configuration
public class SecurityConfiguration {
    
    /**
     * Creates BCryptPasswordEncoder bean.
     * 
     * <p>This bean will be automatically injected wherever needed.
     * Spring will create ONE instance and reuse it (Singleton).
     * 
     * <p>Usage in services:
     * <pre>
     * private final BCryptPasswordEncoder password_encoder;
     * 
     * String hashed = password_encoder.encode("plain_password");
     * boolean matches = password_encoder.matches("plain", hashed);
     * </pre>
     * 
     * @return configured BCrypt encoder
     */
    @Bean
    public BCryptPasswordEncoder password_encoder() {
        // Strength 10 = 2^10 iterations (good balance security/performance)
        return new BCryptPasswordEncoder(10);
    }
}