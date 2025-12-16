package com.network.projet.ussd.controller;

import com.network.projet.ussd.service.UssdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for USSD requests from Africa's Talking.
 * 
 * <p>This controller handles incoming USSD requests following Africa's Talking API format:
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
 * <p>Response Format:
 * <pre>
 * CON Welcome to PickNDrop...  (Continue - user can respond)
 * END Thank you for using...   (End - session terminates)
 * </pre>
 * 
 * <p>Architecture Flow:
 * <pre>
 * Africa's Talking → Controller → UssdService → Business Logic
 *                        ↓
 *                   Validation
 *                        ↓
 *                   Logging
 *                        ↓
 *                Response (CON/END)
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
    
    private final UssdService ussd_service;
    
    /**
     * Main USSD callback endpoint for Africa's Talking.
     * 
     * <p>Request Example:
     * <pre>
     * POST /ussd/callback
     * sessionId=ATUid_12345
     * serviceCode=*384*96#
     * phoneNumber=+237600000000
     * text=1*2*John
     * </pre>
     * 
     * <p>Response Example:
     * <pre>
     * CON Entrez le nom du destinataire:
     * </pre>
     * 
     * @param session_id unique session identifier from Africa's Talking
     * @param service_code USSD code that was dialed
     * @param phone_number user's phone number in E.164 format (+237XXXXXXXXX)
     * @param text cumulative user input separated by * (empty on first request)
     * @return Mono<String> USSD response formatted as "CON message" or "END message"
     */
    @PostMapping(
        value = "/callback",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> handle_ussd_callback(
            @RequestParam("sessionId") String session_id,
            @RequestParam("serviceCode") String service_code,
            @RequestParam("phoneNumber") String phone_number,
            @RequestParam(value = "text", defaultValue = "") String text) {
        
        // Log incoming request (helps debugging in production)
        log.info("USSD Request - SessionId: {}, Phone: {}, ServiceCode: {}, Text: '{}'",
            session_id, phone_number, service_code, text);
        
        // Extract last user input from cumulative text
        // Africa's Talking sends: "1*2*3" when user typed 1, then 2, then 3
        // We need only the last value: "3"
        String user_input = extract_last_input(text);
        
        log.debug("Extracted user input: '{}'", user_input);
        
        // Process request through service layer
        return ussd_service.process_ussd_request(session_id, phone_number, user_input)
            .map(UssdService.UssdResponse::format) // Convert to "CON msg" or "END msg"
            .doOnSuccess(response -> 
                log.info("USSD Response - SessionId: {}, Response: '{}'", 
                    session_id, truncate_for_log(response)))
            .doOnError(error -> 
                log.error("USSD Error - SessionId: {}, Error: {}", 
                    session_id, error.getMessage(), error))
            .onErrorResume(throwable -> {
                // Fallback response on any error
                log.error("Fatal error in USSD processing", throwable);
                return Mono.just("END Erreur système. Veuillez réessayer plus tard.");
            });
    }
    
    /**
     * Test endpoint for local development and debugging.
     * Simulates Africa's Talking request format.
     * 
     * <p>Usage Example:
     * <pre>
     * POST /ussd/test
     * {
     *   "sessionId": "test-session-123",
     *   "phoneNumber": "+237600000000",
     *   "text": "1*John Doe"
     * }
     * </pre>
     * 
     * @param request test request body
     * @return Mono<String> USSD response
     */
    @PostMapping(
        value = "/test",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> handle_test_request(@RequestBody TestUssdRequest request) {
        log.info("TEST Request - SessionId: {}, Phone: {}, Text: '{}'",
            request.session_id, request.phone_number, request.text);
        
        String user_input = extract_last_input(request.text);
        
        return ussd_service.process_ussd_request(
                request.session_id, 
                request.phone_number, 
                user_input)
            .map(UssdService.UssdResponse::format)
            .doOnSuccess(response -> 
                log.info("TEST Response: '{}'", truncate_for_log(response)));
    }
    
    /**
     * Health check endpoint for monitoring.
     * Used by load balancers and monitoring tools.
     * 
     * @return Mono<String> status message
     */
    @GetMapping("/health")
    public Mono<String> health_check() {
        return Mono.just("USSD Service is running");
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
     * <p>We need only the LAST part for each state.
     * 
     * @param text cumulative input from Africa's Talking
     * @return last input value or empty string
     */
    private String extract_last_input(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // Split by * and get last element
        String[] parts = text.split("\\*");
        return parts[parts.length - 1].trim();
    }
    
    /**
     * Truncates response for logging (avoid huge logs).
     * 
     * @param response full response text
     * @return truncated response
     */
    private String truncate_for_log(String response) {
        int max_length = 100;
        if (response.length() <= max_length) {
            return response;
        }
        return response.substring(0, max_length) + "...";
    }
    
    /**
     * DTO for test endpoint requests.
     * Used only in development/testing.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestUssdRequest {
        private String session_id;
        private String phone_number;
        private String text;
    }
}