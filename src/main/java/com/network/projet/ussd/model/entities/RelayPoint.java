// ============================================
// RelayPoint.java
// ============================================
package com.network.projet.ussd.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * RelayPoint entity representing pickup/drop-off locations.
 * Used for intermediate package handling.
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("relay_point")
public class RelayPoint {
    
    @Id
    private Long id;
    
    @Column("name")
    private String name;
    
    @Column("city")
    private String city;
    
    @Column("address")
    private String address;
    
    @Column("phone")
    private String phone;
    
    @Column("latitude")
    private Double latitude;
    
    @Column("longitude")
    private Double longitude;
    
    @Column("is_active")
    private Boolean is_active;
    
    /**
     * Gets formatted location with coordinates.
     * 
     * @return formatted location string
     */
    public String getLocationInfo() {
        return String.format("%s (%s) - GPS: %.6f, %.6f", 
            name, city, latitude, longitude);
    }
}
