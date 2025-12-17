// ============================================
// State.java
// Generic state model for USSD automaton
// ============================================
package com.network.projet.ussd.model.automata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a state in the USSD automaton.
 * States are defined externally in JSON configuration and loaded dynamically.
 * 
 * <p>Design principles:
 * <ul>
 *   <li>States are data-driven, not hard-coded</li>
 *   <li>Each state has a unique ID for navigation</li>
 *   <li>States can be initial, normal, or final</li>
 *   <li>Display information is separated from logic</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * State welcomeState = State.builder()
 *     .stateId("WELCOME")
 *     .label("Welcome State")
 *     .stateType(StateType.INITIAL)
 *     .displayMessage("Welcome to PickNDrop")
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
public class State {
    
    /**
     * Unique identifier for this state.
     * Must match state IDs in transitions.
     * Example: "WELCOME", "ENTER_NAME", "CONFIRM_SHIPMENT"
     */
    private String stateId;
    
    /**
     * Human-readable label for the state.
     * Used for logging and debugging.
     * Example: "Welcome State", "Name Input State"
     */
    private String label;
    
    /**
     * Type of state (INITIAL, NORMAL, FINAL).
     * Determines state behavior in automaton.
     */
    private StateType stateType;
    
    /**
     * Message to display to user when entering this state.
     * Can contain placeholders for dynamic data.
     * Example: "Enter recipient name:", "Welcome {userName}"
     */
    private String displayMessage;
    
    /**
     * Optional description of what this state does.
     * Used for documentation and development.
     */
    private String description;
    
    /**
     * Validation type required for user input in this state.
     * Maps to validation service methods.
     * Example: "PHONE", "EMAIL", "NAME", "WEIGHT"
     * null = no validation required (menu selection)
     */
    private String validationType;
    
    /**
     * List of available menu options when this is a menu state.
     * Each option maps to a transition trigger.
     */
    @Builder.Default
    private List<MenuOption> menuOptions = new ArrayList<>();
    
    /**
     * Name of business service method to call when processing this state.
     * Example: "createRecipient", "calculatePrice"
     * null = no business logic required
     */
    private String businessServiceMethod;
    
    /**
     * Whether this state requires data from previous states.
     * If true, session context must contain required data.
     */
    @Builder.Default
    private boolean requiresContext = false;
    
    /**
     * List of context keys required from session.
     * Example: ["recipientName", "recipientPhone"]
     */
    @Builder.Default
    private List<String> requiredContextKeys = new ArrayList<>();
    
    /**
     * Key to store user input in session context.
     * Example: "recipientName", "packageWeight"
     * null = don't store input (menu states)
     */
    private String contextStorageKey;
    
    /**
     * Whether this state ends the USSD session.
     * True for final states (success, error, etc.)
     */
    @Builder.Default
    private boolean terminatesSession = false;
    
    /**
     * Checks if this is an initial state.
     * 
     * @return true if state type is INITIAL
     */
    public boolean isInitial() {
        return StateType.INITIAL.equals(stateType);
    }
    
    /**
     * Checks if this is a final state.
     * 
     * @return true if state type is FINAL
     */
    public boolean isFinal() {
        return StateType.FINAL.equals(stateType);
    }
    
    /**
     * Checks if this state requires input validation.
     * 
     * @return true if validation type is defined
     */
    public boolean requiresValidation() {
        return validationType != null && !validationType.trim().isEmpty();
    }
    
    /**
     * Checks if this is a menu state (has options).
     * 
     * @return true if menu options exist
     */
    public boolean isMenuState() {
        return !menuOptions.isEmpty();
    }
    
    /**
     * Gets formatted display message with menu options.
     * 
     * @return complete message to show user
     */
    public String getFormattedDisplayMessage() {
        if (!isMenuState()) {
            return displayMessage;
        }
        
        StringBuilder formatted = new StringBuilder(displayMessage);
        formatted.append("\n");
        
        for (int i = 0; i < menuOptions.size(); i++) {
            MenuOption option = menuOptions.get(i);
            formatted.append(option.getOptionKey())
                    .append(". ")
                    .append(option.getOptionText());
            
            if (i < menuOptions.size() - 1) {
                formatted.append("\n");
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Inner class representing a menu option.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuOption {
        
        /**
         * Key user presses to select this option.
         * Example: "1", "2", "*", "#"
         */
        private String optionKey;
        
        /**
         * Text displayed for this option.
         * Example: "Send package", "Track shipment"
         */
        private String optionText;
        
        /**
         * Transition trigger associated with this option.
         * Used to find next state in automaton.
         */
        private String transitionTrigger;
        
        /**
         * Optional icon or emoji for UI enhancement.
         */
        private String icon;
    }
    
    /**
     * Enumeration of state types.
     */
    public enum StateType {
        /**
         * Initial state - entry point of automaton.
         * Only one initial state per automaton.
         */
        INITIAL,
        
        /**
         * Normal state - intermediate processing state.
         * Most states are this type.
         */
        NORMAL,
        
        /**
         * Final state - terminal state.
         * Ends the session (success, error, timeout).
         */
        FINAL
    }
    
    @Override
    public String toString() {
        return String.format("State{id='%s', label='%s', type=%s, isMenu=%b}", 
            stateId, label, stateType, isMenuState());
    }
}