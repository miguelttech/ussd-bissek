// ============================================
// Account.java (Pour gestion des plans)
// ============================================
package com.network.projet.ussd.model.entities;

import com.network.projet.ussd.model.enums.PricingPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Account entity for user subscription management.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("account")
public class Account {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long user_id;
    
    @Column("pricing_plan")
    private PricingPlan pricing_plan;
    
    @Column("shipments_this_month")
    private Integer shipments_this_month;
    
    @Column("subscription_start_date")
    private LocalDateTime subscription_start_date;
    
    @Column("subscription_end_date")
    private LocalDateTime subscription_end_date;
    
    /**
     * Checks if account has reached shipment limit.
     * 
     * @return true if limit reached
     */
    public boolean hasReachedShipmentLimit() {
        Integer max_shipments = pricing_plan.getMaxShipmentsPerMonth();
        return shipments_this_month >= max_shipments;
    }
    
    /**
     * Checks if subscription is active.
     * 
     * @return true if subscription is valid
     */
    public boolean isSubscriptionActive() {
        LocalDateTime now = LocalDateTime.now();
        return subscription_end_date == null 
            || now.isBefore(subscription_end_date);
    }
}