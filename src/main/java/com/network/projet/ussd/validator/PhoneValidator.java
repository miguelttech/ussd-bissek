package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for phone numbers
 */
public class PhoneValidator implements InputValidator {
    private static final String PHONE_PATTERN = "^[+]?[0-9]{7,15}$";
    private static final Pattern pattern = Pattern.compile(PHONE_PATTERN);
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Phone number cannot be empty";
            return false;
        }
        
        String cleanInput = input.replaceAll("[\\s-()]", "");
        if (!pattern.matcher(cleanInput).matches()) {
            errorMessage = "Invalid phone number format";
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
