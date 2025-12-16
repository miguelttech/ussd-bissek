package com.network.projet.ussd.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * R2DBC configuration for reactive PostgreSQL database access.
 * 
 * <p>R2DBC (Reactive Relational Database Connectivity) is the reactive equivalent
 * of JDBC. It provides non-blocking database access using Reactor.
 * 
 * <p>Key Benefits:
 * <ul>
 *   <li>Non-blocking I/O - doesn't block threads waiting for DB</li>
 *   <li>Better resource utilization - fewer threads needed</li>
 *   <li>Higher scalability - can handle more concurrent requests</li>
 *   <li>Perfect for USSD with thousands of concurrent users</li>
 * </ul>
 * 
 * <p>Connection Pool Configuration:
 * <pre>
 * Initial Size: 5 connections (always ready)
 * Max Size: 20 connections (peak capacity)
 * Max Idle Time: 30 minutes (close unused connections)
 * </pre>
 * 
 * <p>Comparison:
 * <pre>
 * JDBC (Blocking):
 * Thread → Wait for DB → Blocked → Response
 * 1000 users = 1000 threads
 * 
 * R2DBC (Non-blocking):
 * Thread → Subscribe to DB → Continue other work → Callback when ready
 * 1000 users = ~10 threads
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.network.projet.ussd.repository")
@Slf4j
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
    
    /**
     * Connection factory is already auto-configured by Spring Boot
     * from application.properties:
     * 
     * spring.r2dbc.url=r2dbc:postgresql://...
     * spring.r2dbc.username=...
     * spring.r2dbc.password=...
     * 
     * This method is required by AbstractR2dbcConfiguration but the actual
     * factory is injected by Spring Boot auto-configuration.
     */
    @Override
    public ConnectionFactory connectionFactory() {
        // Spring Boot auto-configures this from application.properties
        // We just need to implement the method for the abstract class
        throw new UnsupportedOperationException(
            "ConnectionFactory is auto-configured by Spring Boot");
    }
    
    /**
     * Initializes database schema on application startup.
     * 
     * <p>This bean executes SQL scripts when the application starts:
     * <ul>
     *   <li>data.sql - Creates tables, indexes, and initial data</li>
     * </ul>
     * 
     * <p>IMPORTANT: In production, use Liquibase/Flyway instead of this
     * for proper database version control and migration management.
     * 
     * @param connection_factory the R2DBC connection factory
     * @return initialized ConnectionFactoryInitializer
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connection_factory) {
        log.info("Initializing database schema...");
        
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connection_factory);
        
        // Load and execute data.sql
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("data.sql"));
        
        initializer.setDatabasePopulator(populator);
        
        log.info("Database schema initialization configured");
        return initializer;
    }
}