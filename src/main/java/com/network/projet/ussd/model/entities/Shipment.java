// ============================================
// Shipment.java
// ============================================
package com.network.projet.ussd.model.entities;

import com.network.projet.ussd.model.enums.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shipment entity representing a delivery order.
 * Links sender, recipient, and package with delivery information.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("shipment")
public class Shipment {
    
    @Id
    private Long id;
    
    @Column("sender_id")
    private Long sender_id;
    
    @Column("recipient_id")
    private Long recipient_id;
    
    @Column("package_id")
    private Long package_id;
    
    @Column("created_at")
    private LocalDateTime created_at;
    
    /**
     * Unique tracking identifier.
     * Format: PKND-YYYYMMDD-XXXXX
     */
    @Column("tracking_id")
    private String tracking_id;
    
    @Column("status")
    private ShipmentStatus status;
    
    @Column("total_price")
    private BigDecimal total_price;
    
    @Column("pickup_address")
    private String pickup_address;
    
    @Column("delivery_address")
    private String delivery_address;
    
    /**
     * Generates tracking ID based on date and sequence.
     * 
     * @param sequence sequence number
     * @return formatted tracking ID
     */
    public static String generateTrackingId(Long sequence) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("PKND-%04d%02d%02d-%05d", 
            now.getYear(), 
            now.getMonthValue(), 
            now.getDayOfMonth(), 
            sequence);
    }
    
    public boolean canBeCancelled() {
        return status == ShipmentStatus.PENDING 
            || status == ShipmentStatus.CONFIRMED;
    }
    
    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }
}