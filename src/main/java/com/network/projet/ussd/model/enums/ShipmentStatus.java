package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing shipment statuses.
 * Maps to PostgreSQL ENUM type 'shipment_status'.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum ShipmentStatus {
    PENDING("En attente", 0),
    CONFIRMED("Confirmé", 1),
    IN_TRANSIT("En transit", 2),
    DELIVERED("Livré", 3),
    CANCELLED("Annulé", -1);
    
    private final String display_name;
    private final Integer order_index;
    
    ShipmentStatus(String display_name, Integer order_index) {
        this.display_name = display_name;
        this.order_index = order_index;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public Integer getOrderIndex() {
        return order_index;
    }
    
    /**
     * Checks if status can transition to next status.
     * 
     * @param next_status target status
     * @return true if transition is valid
     */
    public boolean canTransitionTo(ShipmentStatus next_status) {
        if (this == CANCELLED || next_status == CANCELLED) {
            return false;
        }
        return next_status.order_index > this.order_index;
    }
}