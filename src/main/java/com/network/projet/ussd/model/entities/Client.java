package com.network.projet.ussd.model.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Client entity representing a customer who sends packages.
 * Extends User with additional location information.
 * 
 * <p>Maps to the 'client' table in PostgreSQL database.
 * Has a foreign key reference to 'user' table.
 * 
 * <p>Business Rules:
 * <ul>
 *   <li>Client must have a valid city and address</li>
 *   <li>City is used for pricing calculations</li>
 *   <li>Address is required for package pickup</li>
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
@Table("client")
public class Client {
    
    /**
     * Client ID, references user.id.
     * Primary key and foreign key.
     */
    @Id
    private Long id;
    
    /**
     * City where the client is located.
     * Used for delivery routing and pricing.
     * Max length: 100 characters.
     */
    @Column("city")
    private String city;
    
    /**
     * Full address of the client.
     * Used as default pickup location.
     */
    @Column("address")
    private String address;
    
    /**
     * Formats the complete location as "City, Address".
     * 
     * @return formatted location string
     */
    public String getFormattedLocation() {
        if (city == null && address == null) {
            return "No location specified";
        }
        if (city == null) {
            return address;
        }
        if (address == null) {
            return city;
        }
        return city + ", " + address;
    }
    
    /**
     * Checks if client has complete location information.
     * 
     * @return true if both city and address are present
     */
    public boolean hasCompleteLocation() {
        return city != null && !city.trim().isEmpty()
            && address != null && !address.trim().isEmpty();
    }
}