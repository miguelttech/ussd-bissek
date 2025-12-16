package com.network.projet.ussd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Main application class for USSD PickNDrop system.
 * This class bootstraps the Spring Boot reactive application with WebFlux and R2DBC.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Reactive programming with Spring WebFlux</li>
 *   <li>Non-blocking database access with R2DBC</li>
 *   <li>High concurrency support for USSD sessions</li>
 *   <li>Redis-based session management</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@SpringBootApplication
@EnableR2dbcRepositories
public class NetworkProjetUssdApplication {

    /**
     * Application entry point.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(NetworkProjetUssdApplication.class, args);
    }
}