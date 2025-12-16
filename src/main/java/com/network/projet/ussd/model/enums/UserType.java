package com.network.projet.ussd.model.enums;

/**
 * Enumeration representing different types of users in the system.
 * Maps to PostgreSQL ENUM type 'user_type'.
 * 
 * <p>User Types:
 * <ul>
 *   <li>CLIENT - Regular customer sending packages</li>
 *   <li>FREELANCE - Independent delivery person</li>
 *   <li>AGENCY - Delivery agency</li>
 *   <li>DELIVERY_PERSON - Employed delivery person</li>
 * </ul>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
public enum UserType {
    
    /**
     * Regular client who sends packages.
     */
    CLIENT("Client", "Regular customer"),
    
    /**
     * Freelance delivery person.
     */
    FREELANCE("Freelance", "Independent contractor"),
    
    /**
     * Delivery agency organization.
     */
    AGENCY("Agency", "Delivery company"),
    
    /**
     * Employed delivery person.
     */
    DELIVERY_PERSON("Delivery Person", "Company employee");
    
    private final String display_name;
    private final String description;
    
    /**
     * Constructor for UserType enum.
     * 
     * @param display_name human-readable name
     * @param description detailed description
     */
    UserType(String display_name, String description) {
        this.display_name = display_name;
        this.description = description;
    }
    
    /**
     * Gets the display name.
     * 
     * @return display name
     */
    public String getDisplayName() {
        return display_name;
    }
    
    /**
     * Gets the description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Converts string to UserType enum.
     * 
     * @param value string value
     * @return UserType enum or null if invalid
     */
    public static UserType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (UserType type : UserType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        return null;
    }
}