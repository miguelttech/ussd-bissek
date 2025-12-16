// ============================================
// ValidationService.java
// ============================================
package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.ValidationException;
import com.network.projet.ussd.validator.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized validation service.
 * Provides validation for all input types with consistent error handling.
 * 
 * <p>Supported Validations:
 * <ul>
 *   <li>Phone numbers (format and length)</li>
 *   <li>Email addresses (RFC 5322 compliant)</li>
 *   <li>Names (alphabetic with spaces)</li>
 *   <li>Passwords (strength requirements)</li>
 *   <li>Addresses (format and length)</li>
 *   <li>Cities (alphabetic)</li>
 *   <li>Weights (numeric range)</li>
 *   <li>Numeric values (with min/max)</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@Slf4j
public class ValidationService {
    
    // Validators (stateless, can be reused)
    private final PhoneValidator phone_validator = new PhoneValidator();
    private final EmailValidator email_validator = new EmailValidator();
    private final NameValidator name_validator = new NameValidator();
    private final PasswordValidator password_validator = new PasswordValidator();
    private final AddressValidator address_validator = new AddressValidator();
    private final CityValidator city_validator = new CityValidator();
    private final WeightValidator weight_validator = new WeightValidator();
    
    /**
     * Validates phone number.
     * 
     * @param phone phone number
     * @return Mono<String> validated phone or error
     */
    public Mono<String> validate_phone(String phone) {
        return Mono.defer(() -> {
            if (phone_validator.isValid(phone)) {
                return Mono.just(phone.trim());
            }
            return Mono.error(new ValidationException(
                phone_validator.getErrorMessage(), "phone"));
        });
    }
    
    /**
     * Validates email.
     * 
     * @param email email address
     * @return Mono<String> validated email or error
     */
    public Mono<String> validate_email(String email) {
        return Mono.defer(() -> {
            if (email_validator.isValid(email)) {
                return Mono.just(email.trim());
            }
            return Mono.error(new ValidationException(
                email_validator.getErrorMessage(), "email"));
        });
    }
    
    /**
     * Validates name.
     * 
     * @param name person name
     * @return Mono<String> validated name or error
     */
    public Mono<String> validate_name(String name) {
        return Mono.defer(() -> {
            if (name_validator.isValid(name)) {
                return Mono.just(name.trim());
            }
            return Mono.error(new ValidationException(
                name_validator.getErrorMessage(), "name"));
        });
    }
    
    /**
     * Validates password.
     * 
     * @param password password
     * @return Mono<String> validated password or error
     */
    public Mono<String> validate_password(String password) {
        return Mono.defer(() -> {
            if (password_validator.isValid(password)) {
                return Mono.just(password);
            }
            return Mono.error(new ValidationException(
                password_validator.getErrorMessage(), "password"));
        });
    }
    
    /**
     * Validates address.
     * 
     * @param address address string
     * @return Mono<String> validated address or error
     */
    public Mono<String> validate_address(String address) {
        return Mono.defer(() -> {
            if (address_validator.isValid(address)) {
                return Mono.just(address.trim());
            }
            return Mono.error(new ValidationException(
                address_validator.getErrorMessage(), "address"));
        });
    }
    
    /**
     * Validates city.
     * 
     * @param city city name
     * @return Mono<String> validated city or error
     */
    public Mono<String> validate_city(String city) {
        return Mono.defer(() -> {
            if (city_validator.isValid(city)) {
                return Mono.just(city.trim());
            }
            return Mono.error(new ValidationException(
                city_validator.getErrorMessage(), "city"));
        });
    }
    
    /**
     * Validates weight.
     * 
     * @param weight_str weight as string
     * @return Mono<String> validated weight or error
     */
    public Mono<String> validate_weight(String weight_str) {
        return Mono.defer(() -> {
            if (weight_validator.isValid(weight_str)) {
                return Mono.just(weight_str.trim());
            }
            return Mono.error(new ValidationException(
                weight_validator.getErrorMessage(), "weight"));
        });
    }
    
    /**
     * Validates multiple fields at once.
     * Returns all errors in a map.
     * 
     * @param fields map of field names to values
     * @return Mono<Map> empty if valid, error map otherwise
     */
    public Mono<Map<String, String>> validate_multiple(Map<String, String> fields) {
        Map<String, String> errors = new HashMap<>();
        
        fields.forEach((field_name, value) -> {
            InputValidator validator = get_validator_for_field(field_name);
            if (validator != null && !validator.isValid(value)) {
                errors.put(field_name, validator.getErrorMessage());
            }
        });
        
        if (errors.isEmpty()) {
            return Mono.just(Map.of());
        } else {
            return Mono.error(new ValidationException(
                "Multiple validation errors", errors));
        }
    }
    
    /**
     * Gets appropriate validator for field name.
     * 
     * @param field_name field identifier
     * @return validator instance
     */
    private InputValidator get_validator_for_field(String field_name) {
        return switch (field_name.toLowerCase()) {
            case "phone", "phone_number" -> phone_validator;
            case "email" -> email_validator;
            case "name" -> name_validator;
            case "password" -> password_validator;
            case "address" -> address_validator;
            case "city" -> city_validator;
            case "weight" -> weight_validator;
            default -> null;
        };
    }
}