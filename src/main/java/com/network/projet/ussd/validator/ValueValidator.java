package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Generic validator for numeric values
 */
public class ValueValidator implements InputValidator {
    private static final String VALUE_PATTERN = "^-?[0-9]+(\\.[0-9]{1,2})?$";
    private static final Pattern pattern = Pattern.compile(VALUE_PATTERN);
    private String errorMessage = "";
    private Double minValue;
    private Double maxValue;

    public ValueValidator() {
        this.minValue = null;
        this.maxValue = null;
    }

    public ValueValidator(Double minValue, Double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Value cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid value format. Enter a number with up to 2 decimal places";
            return false;
        }
        
        try {
            double value = Double.parseDouble(input.trim());
            
            if (minValue != null && value < minValue) {
                errorMessage = "Value must be at least " + minValue;
                return false;
            }
            
            if (maxValue != null && value > maxValue) {
                errorMessage = "Value must not exceed " + maxValue;
                return false;
            }
        } catch (NumberFormatException e) {
            errorMessage = "Invalid numeric value";
            return false;
        }
        
        errorMessage = "";
        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }
}
