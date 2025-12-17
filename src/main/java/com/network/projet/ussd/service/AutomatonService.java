// ============================================
// AutomatonService.java
// Core service for automaton state management and transitions
// ============================================
package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.exception.InvalidTransitionException;
import com.network.projet.ussd.model.automata.Automaton;
import com.network.projet.ussd.model.automata.State;
import com.network.projet.ussd.model.automata.Transition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing automaton state transitions.
 * This is the core engine that executes the state machine logic.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Find appropriate transitions based on current state and input</li>
 *   <li>Execute state transitions</li>
 *   <li>Validate transition guards</li>
 *   <li>Execute transition actions</li>
 *   <li>Handle epsilon (automatic) transitions</li>
 * </ul>
 * 
 * <p>Workflow:
 * <pre>
 * 1. Receive current state + user input
 * 2. Find matching transition
 * 3. Validate guards (if any)
 * 4. Execute transition actions
 * 5. Return next state
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatonService {
    
    private final AutomatonBuilderService automatonBuilderService;
    private final ValidationService validationService;
    
    /**
     * Processes a state transition.
     * This is the main method called by UssdService.
     * 
     * <p>Algorithm:
     * 1. Get automaton
     * 2. Get current state
     * 3. Find matching transition for user input
     * 4. Validate transition (if required)
     * 5. Execute transition actions
     * 6. Return next state
     * 
     * @param currentStateId current state ID
     * @param userInput user's input text
     * @param sessionContext current session context (for guards)
     * @return Mono with transition result
     */
    public Mono<TransitionResult> processTransition(
            String currentStateId, 
            String userInput, 
            Map<String, Object> sessionContext) {
        
        log.debug("Processing transition from state '{}' with input '{}'", currentStateId, userInput);
        
        return automatonBuilderService.getAutomaton()
            .flatMap(automaton -> {
                // Get current state
                State currentState = automaton.getState(currentStateId);
                if (currentState == null) {
                    return Mono.error(new InvalidStateException(
                        "State not found: " + currentStateId, currentStateId));
                }
                
                // Find matching transition
                Transition transition = automaton.findMatchingTransition(currentStateId, userInput);
                if (transition == null) {
                    return Mono.just(TransitionResult.invalidInput(currentState, 
                        "Invalid option. Please try again."));
                }
                
                // Get next state
                State nextState = automaton.getState(transition.getToStateId());
                if (nextState == null) {
                    return Mono.error(new InvalidStateException(
                        "Target state not found: " + transition.getToStateId(), 
                        transition.getToStateId()));
                }
                
                log.debug("Found transition: {} -> {}", currentStateId, nextState.getStateId());
                
                // Execute transition with validation
                return executeTransition(currentState, nextState, transition, userInput, sessionContext);
            });
    }
    
    /**
     * Executes a transition with validation and actions.
     * 
     * @param currentState source state
     * @param nextState target state
     * @param transition transition to execute
     * @param userInput user's input
     * @param sessionContext session data
     * @return Mono with transition result
     */
    private Mono<TransitionResult> executeTransition(
            State currentState,
            State nextState, 
            Transition transition, 
            String userInput,
            Map<String, Object> sessionContext) {
        
        // Check if validation is required
        if (transition.isRequiresValidation() && transition.getValidationType() != null) {
            return validateInput(userInput, transition.getValidationType())
                .flatMap(validationResult -> {
                    if (!validationResult.isValid()) {
                        // Validation failed - stay in current state
                        return Mono.just(TransitionResult.validationFailed(
                            currentState, validationResult.getErrorMessage()));
                    }
                    
                    // Validation passed - proceed with transition
                    return completeTransition(nextState, transition, userInput, sessionContext);
                });
        }
        
        // No validation required - proceed directly
        return completeTransition(nextState, transition, userInput, sessionContext);
    }
    
    /**
     * Completes transition by executing actions.
     * 
     * @param nextState target state
     * @param transition transition being executed
     * @param userInput user's input
     * @param sessionContext session data
     * @return Mono with transition result
     */
    private Mono<TransitionResult> completeTransition(
            State nextState,
            Transition transition,
            String userInput,
            Map<String, Object> sessionContext) {
        
        // Execute transition actions if any
        if (transition.hasActions()) {
            Map<String, Object> actions = executeActions(transition.getActions(), userInput, sessionContext);
            return Mono.just(TransitionResult.success(nextState, actions));
        }
        
        return Mono.just(TransitionResult.success(nextState));
    }
    
    /**
     * Validates user input based on validation type.
     * 
     * @param userInput input to validate
     * @param validationType type of validation
     * @return Mono with validation result
     */
    private Mono<ValidationResult> validateInput(String userInput, String validationType) {
        log.debug("Validating input with type: {}", validationType);
        
        return switch (validationType) {
            case "NAME" -> validationService.validateName(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "PHONE" -> validationService.validatePhone(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "EMAIL" -> validationService.validateEmail(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "EMAIL_OPTIONAL" -> {
                if ("0".equals(userInput.trim())) {
                    yield Mono.just(ValidationResult.success());
                }
                yield validationService.validateEmail(userInput)
                    .map(valid -> ValidationResult.success())
                    .onErrorResume(throwable -> 
                        Mono.just(ValidationResult.failed(throwable.getMessage())));
            }
                
            case "CITY" -> validationService.validateCity(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "ADDRESS" -> validationService.validateAddress(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "WEIGHT" -> validationService.validateWeight(userInput)
                .map(valid -> ValidationResult.success())
                .onErrorResume(throwable -> 
                    Mono.just(ValidationResult.failed(throwable.getMessage())));
                
            case "DESCRIPTION" -> Mono.just(
                userInput != null && userInput.trim().length() >= 5
                    ? ValidationResult.success()
                    : ValidationResult.failed("Description must be at least 5 characters"));
                
            case "DECLARED_VALUE" -> Mono.just(
                isValidDecimal(userInput) && Double.parseDouble(userInput) > 0
                    ? ValidationResult.success()
                    : ValidationResult.failed("Invalid value. Enter a positive number."));
                
            case "TRACKING_ID" -> Mono.just(
                userInput != null && userInput.matches("PKND-\\d{8}-\\d{5}")
                    ? ValidationResult.success()
                    : ValidationResult.failed("Invalid tracking ID format"));
                
            default -> Mono.just(ValidationResult.success()); // No validation
        };
    }
    
    /**
     * Executes transition actions.
     * Actions are in format: "actionType:parameters"
     * Example: "storeInSession:transportMode=TRUCK"
     * 
     * @param actions list of actions to execute
     * @param userInput user's input
     * @param sessionContext session data
     * @return map of actions executed (for storing in session)
     */
    private Map<String, Object> executeActions(
            List<String> actions, 
            String userInput, 
            Map<String, Object> sessionContext) {
        
        Map<String, Object> actionResults = new HashMap<>();
        
        for (String action : actions) {
            log.debug("Executing action: {}", action);
            
            String[] parts = action.split(":", 2);
            if (parts.length != 2) {
                log.warn("Invalid action format: {}", action);
                continue;
            }
            
            String actionType = parts[0];
            String actionParams = parts[1];
            
            switch (actionType) {
                case "storeInSession" -> {
                    // Format: "storeInSession:key=value"
                    String[] keyValue = actionParams.split("=", 2);
                    if (keyValue.length == 2) {
                        actionResults.put(keyValue[0], keyValue[1]);
                        log.debug("Action result: {} = {}", keyValue[0], keyValue[1]);
                    }
                }
                
                case "logEvent" -> {
                    // Format: "logEvent:eventName"
                    log.info("Event logged: {}", actionParams);
                }
                
                default -> log.warn("Unknown action type: {}", actionType);
            }
        }
        
        return actionResults;
    }
    
    /**
     * Checks if a string is a valid decimal number.
     */
    private boolean isValidDecimal(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Gets initial state of automaton.
     * Used when starting new USSD session.
     * 
     * @return Mono with initial state
     */
    public Mono<State> getInitialState() {
        return automatonBuilderService.getAutomaton()
            .map(Automaton::getInitialState)
            .doOnSuccess(state -> log.debug("Retrieved initial state: {}", state.getStateId()));
    }
    
    /**
     * Gets a specific state by ID.
     * 
     * @param stateId state identifier
     * @return Mono with state
     */
    public Mono<State> getState(String stateId) {
        return automatonBuilderService.getAutomaton()
            .map(automaton -> automaton.getState(stateId))
            .flatMap(state -> state != null 
                ? Mono.just(state)
                : Mono.error(new InvalidStateException("State not found: " + stateId, stateId)));
    }
    
    // ============================================
    // INNER CLASSES - DTOs
    // ============================================
    
    /**
     * Result of a transition execution.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class TransitionResult {
        private State nextState;
        private boolean success;
        private String errorMessage;
        private Map<String, Object> actionData;
        
        public static TransitionResult success(State nextState) {
            return new TransitionResult(nextState, true, null, new HashMap<>());
        }
        
        public static TransitionResult success(State nextState, Map<String, Object> actionData) {
            return new TransitionResult(nextState, true, null, actionData);
        }
        
        public static TransitionResult validationFailed(State currentState, String errorMessage) {
            return new TransitionResult(currentState, false, errorMessage, new HashMap<>());
        }
        
        public static TransitionResult invalidInput(State currentState, String errorMessage) {
            return new TransitionResult(currentState, false, errorMessage, new HashMap<>());
        }
    }
    
    /**
     * Result of input validation.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failed(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}