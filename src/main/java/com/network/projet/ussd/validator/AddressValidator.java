package com.network.projet.ussd.validator;

import java.util.regex.Pattern;

/**
 * Validator for addresses
 */
public class AddressValidator implements InputValidator {
    private static final String ADDRESS_PATTERN = "^[a-zA-Z0-9\\s,.'\\-]{5,100}$";
    private static final Pattern pattern = Pattern.compile(ADDRESS_PATTERN);
    private String errorMessage = "";

    @Override
    public boolean isValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            errorMessage = "Address cannot be empty";
            return false;
        }
        
        if (!pattern.matcher(input.trim()).matches()) {
            errorMessage = "Invalid address format. Please enter a valid address";
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
