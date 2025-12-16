// ============================================
// PricingPlan.java
// ============================================
package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing pricing plans for users.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum PricingPlan {
    FREE("Free", 0.0, 5),
    STANDARD("Standard", 5000.0, 50),
    PRO("Pro", 15000.0, 200),
    ENTERPRISE("Entreprise", 50000.0, Integer.MAX_VALUE);
    
    private final String display_name;
    private final Double monthly_cost_xaf;
    private final Integer max_shipments_per_month;
    
    PricingPlan(String display_name, Double monthly_cost_xaf, 
                Integer max_shipments_per_month) {
        this.display_name = display_name;
        this.monthly_cost_xaf = monthly_cost_xaf;
        this.max_shipments_per_month = max_shipments_per_month;
    }
    
    public String getDisplayName() {
        return display_name;
    }
    
    public Double getMonthlyCostXaf() {
        return monthly_cost_xaf;
    }
    
    public Integer getMaxShipmentsPerMonth() {
        return max_shipments_per_month;
    }
}