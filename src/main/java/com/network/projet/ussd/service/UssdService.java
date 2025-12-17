// ============================================
// UssdService.java
// Main USSD orchestration service using automaton architecture
// ============================================
package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.SessionExpiredException;
import com.network.projet.ussd.model.automata.State;
import com.network.projet.ussd.service.AutomatonService.TransitionResult;
import com.network.projet.ussd.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Main USSD service orchestrating the complete flow using automaton architecture.
 * This service is the bridge between USSD requests and the automaton engine.
 * 
 * <p>Architecture:
 * <pre>
 * Africa's Talking → UssdController → UssdService → AutomatonService
 *                                          ↓
 *                                    SessionService
 *                                          ↓
 *                                   Business Services
 * </pre>
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Receive USSD requests and parse input</li>
 *   <li>Manage session lifecycle</li>
 *   <li>Delegate state transitions to AutomatonService</li>
 *   <li>Execute business logic when required</li>
 *   <li>Build USSD responses (CON/END)</li>
 * </ul>
 * 
 * <p>Flow:
 * <pre>
 * 1. Check session validity
 * 2. Get current state from session
 * 3. Process transition via AutomatonService
 * 4. Execute business service if needed
 * 5. Store context data in session
 * 6. Build and return USSD response
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UssdService {
    
    private final SessionService sessionService;
    private final AutomatonService automatonService;
    private final UserService userService;
    private final ShipmentService shipmentService;
    private final PricingService pricingService;
    
    /**
     * Main entry point for USSD requests.
     * Processes request through automaton and returns formatted response.
     * 
     * @param sessionId session identifier from Africa's Talking
     * @param phoneNumber user's phone number
     * @param userInput user's text input (empty on first call)
     * @return Mono with USSD response
     */
    public Mono<UssdResponse> processUssdRequest(
            String sessionId, 
            String phoneNumber, 
            String userInput) {
        
        log.info("Processing USSD request - Session: {}, Phone: {}, Input: '{}'", 
            sessionId, phoneNumber, userInput);
        
        // Check if session exists and is valid
        return sessionService.isValidSession(sessionId)
            .flatMap(isValid -> {
                if (!isValid) {
                    // New session - initialize
                    return handleNewSession(sessionId, phoneNumber);
                } else {
                    // Existing session - process input
                    return handleExistingSession(sessionId, phoneNumber, userInput);
                }
            })
            .onErrorResume(SessionExpiredException.class, error -> {
                // Session expired - restart
                log.warn("Session expired: {}", sessionId);
                return handleNewSession(sessionId, phoneNumber);
            })
            .onErrorResume(throwable -> {
                // Generic error - show error message
                log.error("Error processing USSD request", throwable);
                return Mono.just(UssdResponse.end("System error. Please try again later."));
            });
    }
    
    /**
     * Handles new session initialization.
     * Creates session and shows initial state.
     * 
     * @param sessionId session identifier
     * @param phoneNumber user's phone number
     * @return Mono with welcome response
     */
    private Mono<UssdResponse> handleNewSession(String sessionId, String phoneNumber) {
        log.info("Initializing new session: {}", sessionId);
        
        return sessionService.createSession(phoneNumber)
            .then(automatonService.getInitialState())
            .flatMap(initialState -> {
                // Store initial state in session
                return sessionService.updateSession(sessionId, "currentStateId", initialState.getStateId())
                    .then(sessionService.updateSession(sessionId, "phoneNumber", phoneNumber))
                    .then(checkUserAuthentication(sessionId, phoneNumber))
                    .thenReturn(buildStateResponse(initialState, null));
            });
    }
    
    /**
     * Checks if user is authenticated and stores user ID in session.
     * 
     * @param sessionId session identifier
     * @param phoneNumber user's phone
     * @return Mono completion
     */
    private Mono<Void> checkUserAuthentication(String sessionId, String phoneNumber) {
        return userService.findByPhone(phoneNumber)
            .flatMap(user -> 
                sessionService.updateSession(sessionId, "userId", user.getId()))
            .then()
            .onErrorResume(throwable -> {
                // User not found - they'll need to register
                log.debug("User not found for phone: {}", phoneNumber);
                return Mono.empty();
            });
    }
    
    /**
     * Handles existing session with user input.
     * 
     * @param sessionId session identifier
     * @param phoneNumber user's phone
     * @param userInput user's input
     * @return Mono with next state response
     */
    private Mono<UssdResponse> handleExistingSession(
            String sessionId, 
            String phoneNumber, 
            String userInput) {
        
        log.debug("Processing existing session: {}", sessionId);
        
        return sessionService.getSession(sessionId)
            .flatMap(sessionData -> {
                String currentStateId = (String) sessionData.get("currentStateId");
                
                if (currentStateId == null) {
                    log.error("No current state in session: {}", sessionId);
                    return handleNewSession(sessionId, phoneNumber);
                }
                
                log.debug("Current state: {}, Input: '{}'", currentStateId, userInput);
                
                // Process transition through automaton
                return processStateTransition(sessionId, currentStateId, userInput, sessionData);
            });
    }
    
    /**
     * Processes state transition using automaton service.
     * 
     * @param sessionId session identifier
     * @param currentStateId current state ID
     * @param userInput user's input
     * @param sessionData session context
     * @return Mono with response
     */
    private Mono<UssdResponse> processStateTransition(
            String sessionId,
            String currentStateId,
            String userInput,
            Map<String, Object> sessionData) {
        
        // Get session context for automaton
        Map<String, Object> context = extractSessionContext(sessionData);
        
        return automatonService.processTransition(currentStateId, userInput, context)
            .flatMap(transitionResult -> {
                if (!transitionResult.isSuccess()) {
                    // Transition failed (validation error, invalid input)
                    return Mono.just(buildErrorResponse(
                        transitionResult.getNextState(), 
                        transitionResult.getErrorMessage()));
                }
                
                State nextState = transitionResult.getNextState();
                
                // Store action data in session
                return storeActionData(sessionId, transitionResult.getActionData())
                    .then(storeUserInput(sessionId, nextState, userInput))
                    .then(sessionService.updateSession(sessionId, "currentStateId", nextState.getStateId()))
                    .then(executeBusinessLogic(sessionId, nextState, sessionData))
                    .thenReturn(buildStateResponse(nextState, null));
            });
    }
    
    /**
     * Extracts session context for automaton processing.
     * 
     * @param sessionData full session data
     * @return context map
     */
    private Map<String, Object> extractSessionContext(Map<String, Object> sessionData) {
        Map<String, Object> context = new HashMap<>();
        
        // Extract all stored answers
        if (sessionData.containsKey("answers")) {
            @SuppressWarnings("unchecked")
            Map<String, String> answers = (Map<String, String>) sessionData.get("answers");
            context.putAll(answers);
        }
        
        // Add user ID if available
        if (sessionData.containsKey("userId")) {
            context.put("userId", sessionData.get("userId"));
        }
        
        return context;
    }
    
    /**
     * Stores action data from transition in session.
     * 
     * @param sessionId session identifier
     * @param actionData data from transition actions
     * @return Mono completion
     */
    private Mono<Void> storeActionData(String sessionId, Map<String, Object> actionData) {
        if (actionData == null || actionData.isEmpty()) {
            return Mono.empty();
        }
        
        return sessionService.getSession(sessionId)
            .flatMap(sessionData -> {
                @SuppressWarnings("unchecked")
                Map<String, String> answers = (Map<String, String>) 
                    sessionData.getOrDefault("answers", new HashMap<>());
                
                // Merge action data into answers
                actionData.forEach((key, value) -> 
                    answers.put(key, value.toString()));
                
                sessionData.put("answers", answers);
                return sessionService.updateSession(sessionId, "answers", answers);
            });
    }
    
    /**
     * Stores user input in session context if state requires it.
     * 
     * @param sessionId session identifier
     * @param state current state
     * @param userInput user's input
     * @return Mono completion
     */
    private Mono<Void> storeUserInput(String sessionId, State state, String userInput) {
        if (state.getContextStorageKey() == null) {
            return Mono.empty();
        }
        
        return sessionService.storeAnswer(sessionId, state.getContextStorageKey(), userInput);
    }
    
    /**
     * Executes business logic if state requires it.
     * Calls appropriate business service method.
     * 
     * @param sessionId session identifier
     * @param state current state
     * @param sessionData session context
     * @return Mono completion
     */
    private Mono<Void> executeBusinessLogic(
            String sessionId, 
            State state, 
            Map<String, Object> sessionData) {
        
        if (state.getBusinessServiceMethod() == null) {
            return Mono.empty();
        }
        
        String method = state.getBusinessServiceMethod();
        log.debug("Executing business service method: {}", method);
        
        return switch (method) {
            case "generateShipmentSummary" -> generateShipmentSummary(sessionId, sessionData);
            case "createShipment" -> createShipmentFromSession(sessionId, sessionData);
            case "getPackageTrackingInfo" -> getPackageTrackingInfo(sessionId, sessionData);
            default -> {
                log.warn("Unknown business service method: {}", method);
                yield Mono.empty();
            }
        };
    }
    
    /**
     * Generates shipment summary for confirmation.
     * 
     * @param sessionId session identifier
     * @param sessionData session context
     * @return Mono completion
     */
    private Mono<Void> generateShipmentSummary(String sessionId, Map<String, Object> sessionData) {
        return sessionService.getAllAnswers(sessionId)
            .flatMap(answers -> {
                // Calculate price
                String weight = answers.get("packageWeight");
                String transport = answers.get("transportMode");
                String delivery = answers.get("deliveryType");
                
                // Build summary
                StringBuilder summary = new StringBuilder();
                summary.append("SUMMARY:\n\n");
                summary.append("Recipient: ").append(answers.get("recipientName")).append("\n");
                summary.append("Phone: ").append(answers.get("recipientPhone")).append("\n");
                summary.append("City: ").append(answers.get("recipientCity")).append("\n");
                summary.append("Package: ").append(answers.get("packageDescription")).append("\n");
                summary.append("Weight: ").append(weight).append(" kg\n");
                summary.append("Transport: ").append(transport).append("\n");
                summary.append("Delivery: ").append(delivery).append("\n");
                summary.append("Payment: ").append(answers.get("paymentMethod")).append("\n");
                
                // Store summary
                return sessionService.updateSession(sessionId, "shipmentSummary", summary.toString());
            });
    }
    
    /**
     * Creates shipment from session data.
     * 
     * @param sessionId session identifier
     * @param sessionData session context
     * @return Mono completion
     */
    private Mono<Void> createShipmentFromSession(String sessionId, Map<String, Object> sessionData) {
        // This will be implemented to call ShipmentService
        // For now, just log
        log.info("Creating shipment for session: {}", sessionId);
        return Mono.empty();
    }
    
    /**
     * Gets package tracking information.
     * 
     * @param sessionId session identifier
     * @param sessionData session context
     * @return Mono completion
     */
    private Mono<Void> getPackageTrackingInfo(String sessionId, Map<String, Object> sessionData) {
        return sessionService.getAnswer(sessionId, "trackingId")
            .flatMap(trackingId -> 
                shipmentService.trackShipment(trackingId)
                    .flatMap(shipment -> {
                        String info = String.format(
                            "Tracking: %s\nStatus: %s\nDestination: %s",
                            trackingId,
                            shipment.getStatus().getDisplayName(),
                            shipment.getDeliveryAddress()
                        );
                        return sessionService.updateSession(sessionId, "trackingInfo", info);
                    }))
            .then();
    }
    
    /**
     * Builds USSD response from state.
     * 
     * @param state state to display
     * @param additionalMessage additional message to append
     * @return USSD response
     */
    private UssdResponse buildStateResponse(State state, String additionalMessage) {
        String message = state.getFormattedDisplayMessage();
        
        if (additionalMessage != null) {
            message = additionalMessage + "\n\n" + message;
        }
        
        // Check if this state terminates session
        if (state.isTerminatesSession() || state.isFinal()) {
            return UssdResponse.end(message);
        }
        
        return UssdResponse.continueSession(message);
    }
    
    /**
     * Builds error response.
     * 
     * @param state state to stay in
     * @param errorMessage error message
     * @return USSD response
     */
    private UssdResponse buildErrorResponse(State state, String errorMessage) {
        String message = errorMessage + "\n\n" + state.getFormattedDisplayMessage();
        return UssdResponse.continueSession(message);
    }
    
    // ============================================
    // RESPONSE DTO
    // ============================================
    
    /**
     * USSD Response DTO.
     * Formats responses according to Africa's Talking requirements.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UssdResponse {
        private String type;    // "CON" or "END"
        private String message; // Response text
        
        /**
         * Creates a continue response (user can respond).
         */
        public static UssdResponse continueSession(String message) {
            return new UssdResponse("CON", message);
        }
        
        /**
         * Creates an end response (session terminates).
         */
        public static UssdResponse end(String message) {
            return new UssdResponse("END", message);
        }
        
        /**
         * Formats for Africa's Talking.
         * Returns: "CON message" or "END message"
         */
        public String format() {
            return type + " " + message;
        }
    }
}