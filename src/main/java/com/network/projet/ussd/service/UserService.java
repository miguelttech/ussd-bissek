package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.ValidationException;
import com.network.projet.ussd.model.entities.User;
import com.network.projet.ussd.model.enums.UserType;
import com.network.projet.ussd.repository.UserRepository;
import com.network.projet.ussd.validator.EmailValidator;
import com.network.projet.ussd.validator.PasswordValidator;
import com.network.projet.ussd.validator.PhoneValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for user management operations.
 * Handles user registration, authentication, and profile management.
 * 
 * <p>This service uses reactive programming with Reactor:
 * <ul>
 *   <li>Mono<T> - Returns 0 or 1 element asynchronously</li>
 *   <li>Flux<T> - Returns 0 to N elements asynchronously</li>
 * </ul>
 * 
 * <p>Key Responsibilities:
 * <ul>
 *   <li>User registration with validation</li>
 *   <li>Password hashing and authentication</li>
 *   <li>Phone number uniqueness verification</li>
 *   <li>User lookup for USSD sessions</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    // Injection de dépendances (Spring injecte automatiquement)
    private final UserRepository user_repository;
    private final BCryptPasswordEncoder password_encoder; // Injecté par Spring
    
    // Validators
    private final PhoneValidator phone_validator = new PhoneValidator();
    private final EmailValidator email_validator = new EmailValidator();
    private final PasswordValidator password_validator = new PasswordValidator();
    
    /**
     * Registers a new user in the system.
     * 
     * <p>Process Flow:
     * 1. Validate phone number format
     * 2. Check if phone already exists
     * 3. Validate email if provided
     * 4. Hash password
     * 5. Save user to database
     * 
     * @param name user's full name
     * @param phone phone number (must be unique)
     * @param email email address (optional)
     * @param password plain text password
     * @param role user type
     * @return Mono<User> newly created user
     * @throws ValidationException if validation fails
     */
    public Mono<User> register_user(String name, String phone, String email, 
                                     String password, UserType role) {
        
        log.info("Attempting to register user with phone: {}", phone);
        
        // ÉTAPE 1: Valider le téléphone
        return Mono.defer(() -> {
            if (!phone_validator.isValid(phone)) {
                return Mono.error(new ValidationException(
                    phone_validator.getErrorMessage(), "phone"));
            }
            
            // ÉTAPE 2: Vérifier si le téléphone existe déjà
            return user_repository.existsByPhone(phone)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ValidationException(
                            "Phone number already registered", "phone"));
                    }
                    
                    // ÉTAPE 3: Valider l'email si fourni
                    if (email != null && !email.trim().isEmpty()) {
                        if (!email_validator.isValid(email)) {
                            return Mono.error(new ValidationException(
                                email_validator.getErrorMessage(), "email"));
                        }
                        
                        return user_repository.existsByEmail(email)
                            .flatMap(email_exists -> {
                                if (email_exists) {
                                    return Mono.error(new ValidationException(
                                        "Email already registered", "email"));
                                }
                                return create_and_save_user(name, phone, email, password, role);
                            });
                    }
                    
                    return create_and_save_user(name, phone, email, password, role);
                });
        });
    }
    
    /**
     * Helper method to create and save user.
     * Separated for code reusability.
     * 
     * @return Mono<User> saved user
     */
    private Mono<User> create_and_save_user(String name, String phone, String email,
                                            String password, UserType role) {
        
        // Valider le mot de passe
        if (!password_validator.isValid(password)) {
            return Mono.error(new ValidationException(
                password_validator.getErrorMessage(), "password"));
        }
        
        // Créer l'objet User avec Builder pattern
        User user = User.builder()
            .name(name)
            .phone(phone)
            .email(email)
            .password(password_encoder.encode(password)) // Hash le mot de passe
            .role(role)
            .created_at(LocalDateTime.now())
            .build();
        
        // Sauvegarder dans la BDD (de manière réactive)
        return user_repository.save(user)
            .doOnSuccess(saved_user -> 
                log.info("User registered successfully: {}", saved_user.getId()))
            .doOnError(error -> 
                log.error("Failed to register user: {}", error.getMessage()));
    }
    
    /**
     * Finds user by phone number.
     * Used for USSD session initialization.
     * 
     * @param phone phone number
     * @return Mono<User> user if found, empty otherwise
     */
    public Mono<User> find_by_phone(String phone) {
        log.debug("Looking up user by phone: {}", phone);
        
        return user_repository.findByPhone(phone)
            .doOnSuccess(user -> {
                if (user != null) {
                    log.debug("User found: {}", user.getId());
                } else {
                    log.debug("No user found with phone: {}", phone);
                }
            });
    }
    
    /**
     * Authenticates user with phone and password.
     * 
     * <p>Process:
     * 1. Find user by phone
     * 2. Verify password hash
     * 3. Return user if valid, error otherwise
     * 
     * @param phone phone number
     * @param password plain text password
     * @return Mono<User> authenticated user
     */
    public Mono<User> authenticate(String phone, String password) {
        log.info("Authenticating user: {}", phone);
        
        return user_repository.findByPhone(phone)
            .flatMap(user -> {
                // Vérifier le mot de passe hashé
                if (password_encoder.matches(password, user.getPassword())) {
                    log.info("Authentication successful for user: {}", user.getId());
                    return Mono.just(user);
                } else {
                    log.warn("Authentication failed: invalid password");
                    return Mono.error(new ValidationException(
                        "Invalid credentials", "password"));
                }
            })
            .switchIfEmpty(Mono.error(new ValidationException(
                "User not found", "phone")));
    }
    
    /**
     * Gets user by ID.
     * 
     * @param user_id user identifier
     * @return Mono<User> user or empty
     */
    public Mono<User> find_by_id(Long user_id) {
        return user_repository.findById(user_id);
    }
    
    /**
     * Checks if phone number is available.
     * 
     * @param phone phone number
     * @return Mono<Boolean> true if available
     */
    public Mono<Boolean> is_phone_available(String phone) {
        return user_repository.existsByPhone(phone)
            .map(exists -> !exists); // Inverse: existe = non disponible
    }
    
    /**
     * Gets all users by role.
     * 
     * @param role user type
     * @return Flux<User> stream of users
     */
    public Flux<User> find_by_role(UserType role) {
        return user_repository.findByRole(role);
    }
    
    /**
     * Updates user information.
     * 
     * @param user_id user identifier
     * @param name new name (optional)
     * @param email new email (optional)
     * @return Mono<User> updated user
     */
    public Mono<User> update_user(Long user_id, String name, String email) {
        return user_repository.findById(user_id)
            .flatMap(user -> {
                // Mise à jour conditionnelle
                if (name != null && !name.trim().isEmpty()) {
                    user.setName(name);
                }
                
                if (email != null && !email.trim().isEmpty()) {
                    if (!email_validator.isValid(email)) {
                        return Mono.error(new ValidationException(
                            email_validator.getErrorMessage(), "email"));
                    }
                    user.setEmail(email);
                }
                
                return user_repository.save(user);
            })
            .doOnSuccess(updated -> 
                log.info("User updated: {}", user_id));
    }
    
    /**
     * Changes user password.
     * 
     * @param user_id user identifier
     * @param old_password current password
     * @param new_password new password
     * @return Mono<Void> completion signal
     */
    public Mono<Void> change_password(Long user_id, String old_password, 
                                       String new_password) {
        
        return user_repository.findById(user_id)
            .flatMap(user -> {
                // Vérifier l'ancien mot de passe
                if (!password_encoder.matches(old_password, user.getPassword())) {
                    return Mono.error(new ValidationException(
                        "Current password is incorrect", "old_password"));
                }
                
                // Valider le nouveau mot de passe
                if (!password_validator.isValid(new_password)) {
                    return Mono.error(new ValidationException(
                        password_validator.getErrorMessage(), "new_password"));
                }
                
                // Hasher et sauvegarder
                user.setPassword(password_encoder.encode(new_password));
                return user_repository.save(user);
            })
            .then() // Convertir Mono<User> en Mono<Void>
            .doOnSuccess(v -> log.info("Password changed for user: {}", user_id));
    }
}