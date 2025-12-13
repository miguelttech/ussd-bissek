package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for email addresses
 */
public class EmailValidator implements InputValidator {
    private static final String EMAIL_PATTERN = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Email cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid email format";
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
