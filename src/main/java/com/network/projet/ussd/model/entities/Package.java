package com.network.projet.ussd.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Package entity representing a parcel to be delivered.
 * Contains physical characteristics and special handling requirements.
 * 
 * <p>Maps to the 'package' table in PostgreSQL database.
 * 
 * <p>Business Rules:
 * <ul>
 *   <li>Weight must be positive and within limits (0.5kg - 500kg)</li>
 *   <li>Fragile, perishable, liquid flags affect handling</li>
 *   <li>Insurance requires declared value</li>
 *   <li>Description required for customs and tracking</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("package")
public class Package {
    
    /**
     * Unique identifier for the package.
     */
    @Id
    private Long id;
    
    /**
     * Description of package contents.
     * Required for tracking and customs.
     */
    @Column("description")
    private String description;
    
    /**
     * Weight in kilograms.
     * Must be positive, max 2 decimal places.
     * Range: 0.5 - 500 kg.
     */
    @Column("weight")
    private BigDecimal weight;
    
    /**
     * Indicates if package contains fragile items.
     * Affects handling and pricing.
     */
    @Column("fragile")
    private Boolean fragile;
    
    /**
     * Indicates if package contains perishable items.
     * Requires expedited delivery.
     */
    @Column("perishable")
    private Boolean perishable;
    
    /**
     * Indicates if package contains liquids.
     * Requires special packaging.
     */
    @Column("liquid")
    private Boolean liquid;
    
    /**
     * Indicates if package is insured.
     * Requires declared_value if true.
     */
    @Column("insured")
    private Boolean insured;
    
    /**
     * Declared monetary value for insurance.
     * Required only if insured is true.
     * Max 2 decimal places.
     */
    @Column("declared_value")
    private BigDecimal declared_value;
    
    /**
     * ID of the sender (client).
     * Foreign key to client.id.
     */
    @Column("sender_id")
    private Long sender_id;
    
    /**
     * Minimum allowed weight in kilograms.
     */
    public static final BigDecimal MIN_WEIGHT = new BigDecimal("0.5");
    
    /**
     * Maximum allowed weight in kilograms.
     */
    public static final BigDecimal MAX_WEIGHT = new BigDecimal("500.0");
    
    /**
     * Checks if package requires special handling.
     * 
     * @return true if package is fragile, perishable, or liquid
     */
    public boolean requiresSpecialHandling() {
        return Boolean.TRUE.equals(fragile) 
            || Boolean.TRUE.equals(perishable) 
            || Boolean.TRUE.equals(liquid);
    }
    
    /**
     * Gets handling requirements as formatted string.
     * 
     * @return comma-separated list of special requirements
     */
    public String getHandlingRequirements() {
        if (!requiresSpecialHandling()) {
            return "Standard handling";
        }
        
        StringBuilder requirements = new StringBuilder();
        
        if (Boolean.TRUE.equals(fragile)) {
            requirements.append("Fragile");
        }
        if (Boolean.TRUE.equals(perishable)) {
            if (requirements.length() > 0) requirements.append(", ");
            requirements.append("Perishable");
        }
        if (Boolean.TRUE.equals(liquid)) {
            if (requirements.length() > 0) requirements.append(", ");
            requirements.append("Contains liquids");
        }
        
        return requirements.toString();
    }
    
    /**
     * Validates if weight is within acceptable range.
     * 
     * @return true if weight is valid
     */
    public boolean isWeightValid() {
        if (weight == null) {
            return false;
        }
        return weight.compareTo(MIN_WEIGHT) >= 0 
            && weight.compareTo(MAX_WEIGHT) <= 0;
    }
    
    /**
     * Checks if insurance information is complete.
     * 
     * @return true if insured with declared value, or not insured
     */
    public boolean isInsuranceValid() {
        if (Boolean.TRUE.equals(insured)) {
            return declared_value != null 
                && declared_value.compareTo(BigDecimal.ZERO) > 0;
        }
        return true;
    }
    
    /**
     * Gets package summary for display.
     * 
     * @return formatted package summary
     */
    public String getSummary() {
        return String.format("%s (%.2f kg)%s", 
            description, 
            weight, 
            requiresSpecialHandling() ? " - Special handling required" : "");
    }
}