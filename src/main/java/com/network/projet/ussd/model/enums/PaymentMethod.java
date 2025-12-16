
// ============================================
// PaymentMethod.java
// ============================================
package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing payment methods.
 * Maps to PostgreSQL ENUM type 'payment_method'.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum PaymentMethod {
    CASH("Espèces", true),
    MOBILE_MONEY("Mobile Money", false),
    ORANGE_MONEY("Orange Money", false),
    PAID_BY_RECIPIENT("Payé par destinataire", true);
    
    private final String display_name;
    private final Boolean requires_physical_payment;
    
    PaymentMethod(String display_name, Boolean requires_physical_payment) {
        this.display_name = display_name;
        this.requires_physical_payment = requires_physical_payment;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public Boolean getRequiresPhysicalPayment() {
        return requires_physical_payment;
    }
}
