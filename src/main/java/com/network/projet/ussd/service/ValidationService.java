package com.network.projet.ussd.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    
    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(237)?6[0-9]{8}$"
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[a-zA-ZÀ-ÿ\\s'-]{2,50}$"
    );
    
    /**
     * Valide l'entrée selon le type de validation demandé
     */
    public boolean validate(String validationType, String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        switch (validationType.toLowerCase()) {
            case "nomvalide":
            case "namevalid":
                return validateName(input);
            
            case "emailvalide":
            case "emailvalid":
                return validateEmail(input);
            
            case "numvalide":
            case "telvalide":
            case "phonevalid":
                return validatePhone(input);
            
            case "villevalide":
            case "cityvalid":
                return validateCity(input);
            
            case "adressevalide":
            case "addressvalid":
                return validateAddress(input);
            
            case "descvalide":
            case "descriptionvalid":
                return validateDescription(input);
            
            case "poidsvalide":
            case "weightvalid":
                return validateWeight(input);
            
            case "valvalide":
            case "valuevalid":
                return validateValue(input);
            
            case "mdpvalide":
            case "passwordvalid":
                return validatePassword(input);
            
            default:
                // Par défaut, accepte toute entrée non vide
                return true;
        }
    }
    
    /**
     * Valide un nom
     */
    public boolean validateName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Valide un email
     */
    public boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Valide un numéro de téléphone
     */
    public boolean validatePhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Valide un nom de ville
     */
    public boolean validateCity(String city) {
        return city != null && city.length() >= 2 && city.length() <= 50;
    }
    
    /**
     * Valide une adresse
     */
    public boolean validateAddress(String address) {
        return address != null && address.length() >= 5 && address.length() <= 200;
    }
    
    /**
     * Valide une description
     */
    public boolean validateDescription(String description) {
        return description != null && description.length() >= 3 && description.length() <= 500;
    }
    
    /**
     * Valide un poids (en kg)
     */
    public boolean validateWeight(String weight) {
        try {
            double w = Double.parseDouble(weight);
            return w > 0 && w <= 1000; // max 1 tonne
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Valide une valeur déclarée (en FCFA)
     */
    public boolean validateValue(String value) {
        try {
            double v = Double.parseDouble(value);
            return v >= 0 && v <= 10000000; // max 10 millions FCFA
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Valide un mot de passe
     */
    public boolean validatePassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 50;
    }
}