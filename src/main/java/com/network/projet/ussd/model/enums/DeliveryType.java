// ============================================
// DeliveryType.java
// ============================================
package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing delivery speed types.
 * Maps to PostgreSQL ENUM type 'delivery_type'.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum DeliveryType {
    STANDARD("Standard", 3, 1.0),
    EXPRESS_48H("Express 48h", 2, 1.5),
    EXPRESS_24H("Express 24h", 1, 2.0);
    
    private final String display_name;
    private final Integer delivery_days;
    private final Double price_multiplier;
    
    DeliveryType(String display_name, Integer delivery_days, Double price_multiplier) {
        this.display_name = display_name;
        this.delivery_days = delivery_days;
        this.price_multiplier = price_multiplier;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public Integer getDeliveryDays() {
        return delivery_days;
    }
    
    public Double getPriceMultiplier() {
        return price_multiplier;
    }
}
