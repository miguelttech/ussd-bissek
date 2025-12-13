package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for weight values
 */
public class WeightValidator implements InputValidator {
    private static final String WEIGHT_PATTERN = "^[0-9]+(\\.[0-9]{1,2})?$";
    private static final Pattern pattern = Pattern.compile(WEIGHT_PATTERN);
    private String errorMessage = "";
    private static final double MIN_WEIGHT = 0.5;
    private static final double MAX_WEIGHT = 500.0;

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Weight cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid weight format. Enter a number with up to 2 decimal places";
            return false;
        }
        
        try {
            double weight = Double.parseDouble(input.trim());
            if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
                errorMessage = "Weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT;
                return false;
            }
        } catch (NumberFormatException e) {
            errorMessage = "Invalid weight value";
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
