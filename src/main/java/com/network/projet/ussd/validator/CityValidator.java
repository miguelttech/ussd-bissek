package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for city names
 */
public class CityValidator implements InputValidator {
    private static final String CITY_PATTERN = "^[a-zA-Z\\s'-]{2,50}$";
    private static final Pattern pattern = Pattern.compile(CITY_PATTERN);
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "City cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid city format. Only letters, spaces, hyphens and apostrophes allowed";
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
