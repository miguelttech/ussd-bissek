// ============================================
// TransportMode.java
// ============================================
package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing transport modes for delivery.
 * Maps to PostgreSQL ENUM type 'transport_mode'.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum TransportMode {
    TRUCK("Camion", 500.0),
    TRICYCLE("Tricycle", 100.0),
    MOTORCYCLE("Moto", 50.0),
    BICYCLE("Vélo", 20.0),
    CAR("Voiture", 150.0);
    
    private final String display_name;
    private final Double max_weight_kg;
    
    TransportMode(String display_name, Double max_weight_kg) {
        this.display_name = display_name;
        this.max_weight_kg = max_weight_kg;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public Double getMaxWeightKg() {
        return max_weight_kg;
    }
}
