// ============================================
// Payment.java
// ============================================
package com.network.projet.ussd.model.entities;

import com.network.projet.ussd.model.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Payment entity representing payment information.
 * Linked one-to-one with shipment.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment")
public class Payment {
    
    @Id
    private Long id;
    
    @Column("shipment_id")
    private Long shipment_id;
    
    @Column("mode")
    private PaymentMethod mode;
    
    @Column("amount")
    private BigDecimal amount;
    
    /**
     * Checks if payment requires physical collection.
     * 
     * @return true if cash or recipient payment
     */
    public boolean requiresPhysicalCollection() {
        return mode != null && mode.getRequiresPhysicalPayment();
    }
    
    /**
     * Gets formatted amount with currency.
     * 
     * @return formatted amount string
     */
    public String getFormattedAmount() {
        return String.format("%,.0f XAF", amount);
    }
}
