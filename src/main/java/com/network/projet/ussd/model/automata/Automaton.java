// ============================================
// Automaton.java
// Complete automaton model for USSD flow
// ============================================
package com.network.projet.ussd.model.automata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete finite state automaton for USSD application.
 * This is the core model that defines the entire application flow.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Deterministic finite automaton (DFA)</li>
 *   <li>Loaded dynamically from JSON configuration</li>
 *   <li>States and transitions are data, not code</li>
 *   <li>Supports epsilon transitions (automatic transitions)</li>
 *   <li>Conditional transitions with guards</li>
 *   <li>Priority-based transition selection</li>
 * </ul>
 * 
 * <p>Architecture:
 * <pre>
 * Automaton
 *   ├── States (Map<String, State>)
 *   ├── Transitions (List<Transition>)
 *   ├── Initial State
 *   └── Final States (Set<String>)
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
@Slf4j
public class Automaton {
    
    /**
     * Unique identifier for this automaton.
     * Example: "USSD_MAIN_AUTOMATON", "PAYMENT_FLOW"
     */
    private String automatonId;
    
    /**
     * Human-readable name.
     * Example: "USSD Main Flow", "Package Tracking Automaton"
     */
    private String name;
    
    /**
     * Version of automaton configuration.
     * Used for tracking changes and migrations.
     * Format: "MAJOR.MINOR.PATCH"
     */
    private String version;
    
    /**
     * Optional description of automaton purpose.
     */
    private String description;
    
    /**
     * Map of all states indexed by state ID.
     * Fast O(1) lookup by state ID.
     */
    @Builder.Default
    private Map<String, State> states = new HashMap<>();
    
    /**
     * List of all transitions.
     * Multiple transitions can have same source state.
     */
    @Builder.Default
    private List<Transition> transitions = new ArrayList<>();
    
    /**
     * ID of the initial state.
     * Every automaton execution starts here.
     */
    private String initialStateId;
    
    /**
     * Set of final state IDs.
     * Reaching these states terminates execution.
     */
    @Builder.Default
    private Set<String> finalStateIds = new HashSet<>();
    
    /**
     * Metadata about automaton.
     * Can store configuration, creation date, author, etc.
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    // ============================================
    // STATE OPERATIONS
    // ============================================
    
    /**
     * Gets a state by its ID.
     * 
     * @param stateId state identifier
     * @return state or null if not found
     */
    public State getState(String stateId) {
        return states.get(stateId);
    }
    
    /**
     * Gets initial state.
     * 
     * @return initial state or null if not defined
     */
    public State getInitialState() {
        return getState(initialStateId);
    }
    
    /**
     * Checks if a state exists.
     * 
     * @param stateId state identifier
     * @return true if state exists
     */
    public boolean hasState(String stateId) {
        return states.containsKey(stateId);
    }
    
    /**
     * Checks if a state is final.
     * 
     * @param stateId state identifier
     * @return true if state is in final states set
     */
    public boolean isFinalState(String stateId) {
        return finalStateIds.contains(stateId);
    }
    
    /**
     * Adds a state to automaton.
     * 
     * @param state state to add
     */
    public void addState(State state) {
        if (state != null && state.getStateId() != null) {
            states.put(state.getStateId(), state);
            
            if (state.isFinal()) {
                finalStateIds.add(state.getStateId());
            }
            
            log.debug("Added state: {}", state.getStateId());
        }
    }
    
    // ============================================
    // TRANSITION OPERATIONS
    // ============================================
    
    /**
     * Gets all transitions from a given state.
     * 
     * @param fromStateId source state ID
     * @return list of outgoing transitions
     */
    public List<Transition> getTransitionsFrom(String fromStateId) {
        return transitions.stream()
            .filter(t -> t.getFromStateId().equals(fromStateId))
            .sorted(Transition::compareTo) // Sort by priority
            .collect(Collectors.toList());
    }
    
    /**
     * Finds the best matching transition for given state and input.
     * 
     * <p>Selection algorithm:
     * 1. Get all transitions from current state
     * 2. Filter by matching trigger
     * 3. Sort by priority (highest first)
     * 4. Return first match (or null)
     * 
     * @param currentStateId current state
     * @param userInput user's input
     * @return matching transition or null
     */
    public Transition findMatchingTransition(String currentStateId, String userInput) {
        List<Transition> candidates = getTransitionsFrom(currentStateId);
        
        log.debug("Finding transition from {} with input '{}', {} candidates", 
            currentStateId, userInput, candidates.size());
        
        // First pass: exact matches
        Optional<Transition> exactMatch = candidates.stream()
            .filter(t -> !t.isFallbackTransition())
            .filter(t -> t.matchesTrigger(userInput))
            .findFirst();
        
        if (exactMatch.isPresent()) {
            log.debug("Found exact match: {} -> {}", 
                exactMatch.get().getTrigger(), exactMatch.get().getToStateId());
            return exactMatch.get();
        }
        
        // Second pass: fallback transition
        Optional<Transition> fallback = candidates.stream()
            .filter(Transition::isFallbackTransition)
            .findFirst();
        
        if (fallback.isPresent()) {
            log.debug("Using fallback transition to {}", fallback.get().getToStateId());
            return fallback.get();
        }
        
        log.warn("No matching transition found for state {} with input '{}'", 
            currentStateId, userInput);
        return null;
    }
    
    /**
     * Gets epsilon (automatic) transitions from a state.
     * These transitions activate without user input.
     * 
     * @param fromStateId source state
     * @return list of epsilon transitions
     */
    public List<Transition> getEpsilonTransitions(String fromStateId) {
        return transitions.stream()
            .filter(t -> t.getFromStateId().equals(fromStateId))
            .filter(t -> t.getTrigger() != null && t.getTrigger().isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * Adds a transition to automaton.
     * 
     * @param transition transition to add
     */
    public void addTransition(Transition transition) {
        if (transition != null) {
            transitions.add(transition);
            log.debug("Added transition: {} -[{}]-> {}", 
                transition.getFromStateId(), 
                transition.getTrigger(), 
                transition.getToStateId());
        }
    }
    
    // ============================================
    // VALIDATION
    // ============================================
    
    /**
     * Validates automaton integrity.
     * Checks for common configuration errors.
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Check initial state exists
        if (initialStateId == null || !hasState(initialStateId)) {
            errors.add("Initial state not defined or not found: " + initialStateId);
        }
        
        // Check all final states exist
        for (String finalStateId : finalStateIds) {
            if (!hasState(finalStateId)) {
                errors.add("Final state not found: " + finalStateId);
            }
        }
        
        // Check all transitions reference existing states
        for (Transition transition : transitions) {
            if (!hasState(transition.getFromStateId())) {
                errors.add(String.format("Transition references non-existent source state: %s", 
                    transition.getFromStateId()));
            }
            if (!hasState(transition.getToStateId())) {
                errors.add(String.format("Transition references non-existent target state: %s", 
                    transition.getToStateId()));
            }
        }
        
        // Check for unreachable states
        Set<String> reachableStates = findReachableStates();
        for (String stateId : states.keySet()) {
            if (!reachableStates.contains(stateId) && !stateId.equals(initialStateId)) {
                errors.add("State is unreachable: " + stateId);
            }
        }
        
        return errors;
    }
    
    /**
     * Finds all states reachable from initial state.
     * Uses breadth-first search.
     * 
     * @return set of reachable state IDs
     */
    private Set<String> findReachableStates() {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        queue.add(initialStateId);
        reachable.add(initialStateId);
        
        while (!queue.isEmpty()) {
            String currentStateId = queue.poll();
            
            for (Transition transition : getTransitionsFrom(currentStateId)) {
                String nextStateId = transition.getToStateId();
                if (!reachable.contains(nextStateId)) {
                    reachable.add(nextStateId);
                    queue.add(nextStateId);
                }
            }
        }
        
        return reachable;
    }
    
    // ============================================
    // STATISTICS
    // ============================================
    
    /**
     * Gets statistics about this automaton.
     * 
     * @return statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStates", states.size());
        stats.put("totalTransitions", transitions.size());
        stats.put("initialState", initialStateId);
        stats.put("finalStates", finalStateIds.size());
        stats.put("version", version);
        
        long menuStates = states.values().stream()
            .filter(State::isMenuState)
            .count();
        stats.put("menuStates", menuStates);
        
        long validationStates = states.values().stream()
            .filter(State::requiresValidation)
            .count();
        stats.put("validationStates", validationStates);
        
        return stats;
    }
    
    @Override
    public String toString() {
        return String.format("Automaton{id='%s', name='%s', version='%s', states=%d, transitions=%d}", 
            automatonId, name, version, states.size(), transitions.size());
    }
}