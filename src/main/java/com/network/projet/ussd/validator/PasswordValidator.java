package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for passwords
 */
public class PasswordValidator implements InputValidator {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 50;
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.isEmpty()) {
            errorMessage = "Password cannot be empty";
            return false;
        }
        
        if (input.length() < MIN_LENGTH) {
            errorMessage = "Password must be at least " + MIN_LENGTH + " characters long";
            return false;
        }
        
        if (input.length() > MAX_LENGTH) {
            errorMessage = "Password must not exceed " + MAX_LENGTH + " characters";
            return false;
        }
        
        if (!Pattern.compile("[A-Z]").matcher(input).find()) {
            errorMessage = "Password must contain at least one uppercase letter";
            return false;
        }
        
        if (!Pattern.compile("[a-z]").matcher(input).find()) {
            errorMessage = "Password must contain at least one lowercase letter";
            return false;
        }
        
        if (!Pattern.compile("[0-9]").matcher(input).find()) {
            errorMessage = "Password must contain at least one digit";
            return false;
        }
        
        if (!Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]").matcher(input).find()) {
            errorMessage = "Password must contain at least one special character";
            return false;
        }
        
        errorMessage = "";
        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
