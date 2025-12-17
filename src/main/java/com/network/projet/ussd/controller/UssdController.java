// ============================================
// UssdController.java
// REST Controller for USSD requests with automaton architecture
// ============================================
package com.network.projet.ussd.controller;

import com.network.projet.ussd.service.UssdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for USSD requests from Africa's Talking.
 * Handles incoming requests and delegates to UssdService.
 * 
 * <p>Request format (Africa's Talking):
 * <pre>
 * POST /ussd/callback
 * Content-Type: application/x-www-form-urlencoded
 * 
 * Parameters:
 * - sessionId: Unique session identifier
 * - serviceCode: USSD code dialed (e.g., *384*96#)
 * - phoneNumber: User's phone number in E.164 format
 * - text: User's cumulative input (empty on first request)
 * </pre>
 * 
 * <p>Response format:
 * <pre>
 * CON Welcome to PickNDrop...  (Continue - user can respond)
 * END Thank you for using...   (End - session terminates)
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@RestController
@RequestMapping("/ussd")
@RequiredArgsConstructor
@Slf4j
public class UssdController {
    
    private final UssdService ussdService;
    
    /**
     * Main USSD callback endpoint for Africa's Talking.
     * 
     * @param sessionId unique session identifier from Africa's Talking
     * @param serviceCode USSD code that was dialed
     * @param phoneNumber user's phone number in E.164 format (+237XXXXXXXXX)
     * @param text cumulative user input separated by * (empty on first request)
     * @return Mono with USSD response formatted as "CON message" or "END message"
     */
    @PostMapping(
        value = "/callback",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> handleUssdCallback(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("serviceCode") String serviceCode,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "text", defaultValue = "") String text) {
        
        // Log incoming request
        log.info("USSD Request - SessionId: {}, Phone: {}, ServiceCode: {}, Text: '{}'",
            sessionId, phoneNumber, serviceCode, text);
        
        // Extract last user input from cumulative text
        String userInput = extractLastInput(text);
        
        log.debug("Extracted user input: '{}'", userInput);
        
        // Process request through service layer
        return ussdService.processUssdRequest(sessionId, phoneNumber, userInput)
            .map(UssdService.UssdResponse::format)
            .doOnSuccess(response -> 
                log.info("USSD Response - SessionId: {}, Response: '{}'", 
                    sessionId, truncateForLog(response, 100)))
            .doOnError(error -> 
                log.error("USSD Error - SessionId: {}, Error: {}", 
                    sessionId, error.getMessage(), error))
            .onErrorResume(throwable -> {
                // Fallback response on any error
                log.error("Fatal error in USSD processing", throwable);
                return Mono.just("END System error. Please try again later.");
            });
    }
    
    /**
     * Test endpoint for local development and debugging.
     * Simulates Africa's Talking request format.
     * 
     * <p>Usage:
     * <pre>
     * POST /ussd/test
     * Content-Type: application/json
     * {
     *   "sessionId": "test-session-123",
     *   "phoneNumber": "+237600000000",
     *   "text": "1*John Doe"
     * }
     * </pre>
     * 
     * @param request test request body
     * @return Mono with USSD response
     */
    @PostMapping(
        value = "/test",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> handleTestRequest(@RequestBody TestUssdRequest request) {
        log.info("TEST Request - SessionId: {}, Phone: {}, Text: '{}'",
            request.sessionId, request.phoneNumber, request.text);
        
        String userInput = extractLastInput(request.text);
        
        return ussdService.processUssdRequest(
                request.sessionId, 
                request.phoneNumber, 
                userInput)
            .map(UssdService.UssdResponse::format)
            .doOnSuccess(response -> 
                log.info("TEST Response: '{}'", truncateForLog(response, 100)));
    }
    
    /**
     * Health check endpoint for monitoring.
     * 
     * @return Mono with status message
     */
    @GetMapping("/health")
    public Mono<HealthResponse> healthCheck() {
        return Mono.just(new HealthResponse(
            "healthy", 
            "USSD Service is running", 
            System.currentTimeMillis()));
    }
    
    /**
     * Gets automaton information.
     * Useful for debugging and monitoring.
     * 
     * @return Mono with automaton info
     */
    @GetMapping("/automaton/info")
    public Mono<String> getAutomatonInfo() {
        // This could be enhanced to return actual automaton statistics
        return Mono.just("Automaton is loaded and operational");
    }
    
    /**
     * Extracts the last user input from cumulative text.
     * 
     * <p>Africa's Talking sends cumulative input:
     * <ul>
     *   <li>First request: text = "" (empty)</li>
     *   <li>User types "1": text = "1"</li>
     *   <li>User types "2": text = "1*2"</li>
     *   <li>User types "John": text = "1*2*John"</li>
     * </ul>
     * 
     * @param text cumulative input from Africa's Talking
     * @return last input value or empty string
     */
    private String extractLastInput(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // Split by * and get last element
        String[] parts = text.split("\\*");
        return parts[parts.length - 1].trim();
    }
    
    /**
     * Truncates response for logging.
     * 
     * @param response full response text
     * @param maxLength maximum length
     * @return truncated response
     */
    private String truncateForLog(String response, int maxLength) {
        if (response == null) {
            return "";
        }
        if (response.length() <= maxLength) {
            return response;
        }
        return response.substring(0, maxLength) + "...";
    }
    
    // ============================================
    // DTOs
    // ============================================
    
    /**
     * DTO for test endpoint requests.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestUssdRequest {
        private String sessionId;
        private String phoneNumber;
        private String text;
    }
    
    /**
     * DTO for health check response.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private String message;
        private long timestamp;
    }
}