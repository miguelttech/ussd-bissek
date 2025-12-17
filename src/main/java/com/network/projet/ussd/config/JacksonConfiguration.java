// ============================================
// JacksonConfiguration.java
// Configuration for Jackson ObjectMapper
// ============================================
package com.network.projet.ussd.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for Jackson JSON serialization/deserialization.
 * Provides ObjectMapper bean for parsing automaton configuration and other JSON operations.
 * 
 * <p>Features configured:
 * <ul>
 *   <li>Java 8 date/time support (LocalDateTime, etc.)</li>
 *   <li>Pretty printing for development</li>
 *   <li>Fail on unknown properties disabled</li>
 *   <li>Null value handling</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Configuration
public class JacksonConfiguration {
    
    /**
     * Creates and configures ObjectMapper bean.
     * This bean is used throughout the application for JSON operations.
     * 
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8 date/time module for LocalDateTime support
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Enable pretty printing for development
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Don't fail on unknown properties (allows backward compatibility)
        mapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, 
            false);
        
        // Include non-null values only
        mapper.setSerializationInclusion(
            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        return mapper;
    }
}