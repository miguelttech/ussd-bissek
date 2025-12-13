package com.network.projet.ussd.validator;

/**
 * Interface for input validation
 */
public interface InputValidator {
    /**
     * Validates the given input
     * @param input the input to validate
     * @return true if valid, false otherwise
     */
    boolean isValid(String input);
    
    /**
     * Returns an error message if validation fails
     * @return error message
     */
    String getErrorMessage();
}
