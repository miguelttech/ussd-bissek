// ============================================
// Delivery.java
// ============================================
package com.network.projet.ussd.model.entities;

import com.network.projet.ussd.model.enums.DeliveryType;
import com.network.projet.ussd.model.enums.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Delivery entity representing delivery execution details.
 * Links shipment with transport and delivery person.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("delivery")
public class Delivery {
    
    @Id
    private Long id;
    
    @Column("shipment_id")
    private Long shipment_id;
    
    @Column("delivery_person_id")
    private Long delivery_person_id;
    
    @Column("transport")
    private TransportMode transport;
    
    @Column("type")
    private DeliveryType type;
    
    /**
     * Gets estimated delivery time in days.
     * 
     * @return number of days
     */
    public Integer getEstimatedDays() {
        return type != null ? type.getDeliveryDays() : null;
    }
    
    /**
     * Checks if delivery is assigned.
     * 
     * @return true if delivery person is assigned
     */
    public boolean isAssigned() {
        return delivery_person_id != null;
    }
}
