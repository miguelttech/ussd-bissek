// ============================================
// Transition.java
// Generic transition model for USSD automaton
// ============================================
package com.network.projet.ussd.model.automata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a transition between states in the USSD automaton.
 * Transitions define how the automaton moves from one state to another.
 * 
 * <p>Design principles:
 * <ul>
 *   <li>Transitions are data-driven from JSON configuration</li>
 *   <li>Each transition has a trigger (user input or event)</li>
 *   <li>Conditions can be applied for conditional transitions</li>
 *   <li>Guards can prevent invalid transitions</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>
 * Transition nameTransition = Transition.builder()
 *     .fromStateId("ENTER_NAME")
 *     .toStateId("ENTER_PHONE")
 *     .trigger("nameValid")
 *     .requiresValidation(true)
 *     .build();
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transition {
    
    /**
     * Source state ID.
     * The state from which this transition originates.
     * Example: "WELCOME", "ENTER_NAME"
     */
    private String fromStateId;
    
    /**
     * Destination state ID.
     * The state to which this transition leads.
     * Example: "MAIN_MENU", "ENTER_PHONE"
     */
    private String toStateId;
    
    /**
     * Trigger that activates this transition.
     * Can be:
     * <ul>
     *   <li>User input value (e.g., "1", "2", "yes")</li>
     *   <li>Validation result (e.g., "nameValid", "emailInvalid")</li>
     *   <li>System event (e.g., "timeout", "error")</li>
     *   <li>Empty string "" = epsilon transition (automatic)</li>
     * </ul>
     */
    private String trigger;
    
    /**
     * Human-readable label for this transition.
     * Used for logging and debugging.
     * Example: "Valid name entered", "User selected option 1"
     */
    private String label;
    
    /**
     * Optional description of transition purpose.
     */
    private String description;
    
    /**
     * Priority when multiple transitions match.
     * Higher values take precedence.
     * Default: 0
     */
    @Builder.Default
    private int priority = 0;
    
    /**
     * Whether this transition requires input validation.
     * If true, validation must pass before transition occurs.
     */
    @Builder.Default
    private boolean requiresValidation = false;
    
    /**
     * Type of validation to perform.
     * Maps to ValidationService methods.
     * Example: "PHONE", "EMAIL", "NAME", "WEIGHT"
     */
    private String validationType;
    
    /**
     * Guard conditions that must be satisfied.
     * List of condition expressions evaluated at runtime.
     * Example: ["sessionHasKey:recipientName", "weightInRange:0.5-500"]
     */
    @Builder.Default
    private List<String> guardConditions = new ArrayList<>();
    
    /**
     * Actions to execute when transition occurs.
     * Example: ["storeInSession:recipientName", "logEvent:nameEntered"]
     */
    @Builder.Default
    private List<String> actions = new ArrayList<>();
    
    /**
     * Whether this is an error transition.
     * Error transitions handle validation failures or exceptions.
     */
    @Builder.Default
    private boolean isErrorTransition = false;
    
    /**
     * Whether this is a fallback transition.
     * Fallback transitions activate when no other transition matches.
     */
    @Builder.Default
    private boolean isFallbackTransition = false;
    
    /**
     * Error message to display if this error transition activates.
     */
    private String errorMessage;
    
    /**
     * Number of times user can retry on error before forcing exit.
     * -1 = unlimited retries
     * Default: 3
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * Checks if trigger matches user input.
     * 
     * @param userInput user's input string
     * @return true if transition should activate
     */
    public boolean matchesTrigger(String userInput) {
        if (trigger == null) {
            return false;
        }
        
        // Empty trigger = epsilon transition (always matches)
        if (trigger.isEmpty()) {
            return true;
        }
        
        // Wildcard trigger = matches any input
        if ("*".equals(trigger)) {
            return userInput != null && !userInput.trim().isEmpty();
        }
        
        // Exact match
        return trigger.equalsIgnoreCase(userInput != null ? userInput.trim() : "");
    }
    
    /**
     * Checks if this transition has guard conditions.
     * 
     * @return true if guards exist
     */
    public boolean hasGuards() {
        return !guardConditions.isEmpty();
    }
    
    /**
     * Checks if this transition has actions.
     * 
     * @return true if actions exist
     */
    public boolean hasActions() {
        return !actions.isEmpty();
    }
    
    /**
     * Gets validation type or null if no validation required.
     * 
     * @return validation type string
     */
    public String getValidationTypeOrNull() {
        return requiresValidation ? validationType : null;
    }
    
    @Override
    public String toString() {
        return String.format("Transition{%s -[%s]-> %s, priority=%d, validation=%s}", 
            fromStateId, trigger, toStateId, priority, 
            requiresValidation ? validationType : "none");
    }
    
    /**
     * Compares transitions by priority (for sorting).
     * Higher priority transitions come first.
     * 
     * @param other transition to compare with
     * @return comparison result
     */
    public int compareTo(Transition other) {
        return Integer.compare(other.priority, this.priority);
    }
}