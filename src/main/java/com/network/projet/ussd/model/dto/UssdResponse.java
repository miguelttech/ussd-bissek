// ============================================
// UssdResponse.java
// Format Africa's Talking (String simple)
// ============================================
package com.network.projet.ussd.model.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Réponse USSD selon Africa's Talking
 * Retourne simplement "CON message" ou "END message"
 */
public class UssdResponse {
    
    private String response;
    
    public UssdResponse() {
    }
    
    public UssdResponse(String response) {
        this.response = response;
    }
    
    private UssdResponse(boolean isContinue, String message) {
        this.response = (isContinue ? "CON " : "END ") + message;
    }
    
    // Méthodes factory
    
    public static UssdResponse continueSession(String message) {
        return new UssdResponse(true, message);
    }
    
    public static UssdResponse endSession(String message) {
        return new UssdResponse(false, message);
    }
    
    // Cette annotation fait que seul "response" sera sérialisé
    @JsonValue
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public boolean isContinue() {
        return response != null && response.startsWith("CON");
    }
    
    public boolean isEnd() {
        return response != null && response.startsWith("END");
    }
    
    @Override
    public String toString() {
        return response;
    }
}