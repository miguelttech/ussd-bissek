// ============================================
// UserRepository.java
// ============================================
package com.network.projet.ussd.repository;

import com.network.projet.ussd.model.entities.User;
import com.network.projet.ussd.model.enums.UserType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for User entity operations.
 * Provides reactive database access methods.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {
    
    /**
     * Finds user by phone number.
     * Used for USSD authentication and lookup.
     * 
     * @param phone phone number
     * @return Mono with user or empty
     */
    Mono<User> findByPhone(String phone);
    
    /**
     * Finds user by email.
     * 
     * @param email email address
     * @return Mono with user or empty
     */
    Mono<User> findByEmail(String email);
    
    /**
     * Finds all users by role type.
     * 
     * @param role user type
     * @return Flux of users
     */
    Flux<User> findByRole(UserType role);
    
    /**
     * Checks if phone number exists.
     * 
     * @param phone phone number
     * @return Mono with true if exists
     */
    Mono<Boolean> existsByPhone(String phone);
    
    /**
     * Checks if email exists.
     * 
     * @param email email address
     * @return Mono with true if exists
     */
    Mono<Boolean> existsByEmail(String email);
    
    /**
     * Finds users by role and city (for clients).
     * Custom query joining with client table.
     * 
     * @param role user role
     * @param city city name
     * @return Flux of users
     */
    @Query("SELECT u.* FROM \"user\" u " +
           "INNER JOIN client c ON u.id = c.id " +
           "WHERE u.role = :role AND c.city = :city")
    Flux<User> findByRoleAndCity(String role, String city);
}
