package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for names
 */
public class NameValidator implements InputValidator {
    private static final String NAME_PATTERN = "^[a-zA-Z\\s'-]{2,50}$";
    private static final Pattern pattern = Pattern.compile(NAME_PATTERN);
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Name cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid name format. Only letters, spaces, hyphens and apostrophes allowed";
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
