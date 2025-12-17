// ============================================
// UssdRequest.java
// Format Africa's Talking avec WebFlux
// ============================================
package com.network.projet.ussd.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Requête USSD selon le format Africa's Talking
 */
public class UssdRequest {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("serviceCode")
    private String serviceCode;
    
    @JsonProperty("networkCode")
    private String networkCode;
    
    // Constructeurs
    public UssdRequest() {
    }
    
    public UssdRequest(String sessionId, String phoneNumber, String text) {
        this.sessionId = sessionId;
        this.phoneNumber = phoneNumber;
        this.text = text;
    }
    
    // Getters et Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getServiceCode() {
        return serviceCode;
    }
    
    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
    
    public String getNetworkCode() {
        return networkCode;
    }
    
    public void setNetworkCode(String networkCode) {
        this.networkCode = networkCode;
    }
    
    // Méthodes utilitaires
    
    public boolean isInitialRequest() {
        return text == null || text.trim().isEmpty();
    }
    
    /**
     * Récupère la dernière entrée utilisateur
     * Ex: "1*2*5" → retourne "5"
     */
    public String getLastInput() {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String[] inputs = text.split("\\*");
        return inputs[inputs.length - 1];
    }
    
    /**
     * Récupère toutes les entrées sous forme de tableau
     */
    public String[] getInputHistory() {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.split("\\*");
    }
    
    public int getStepCount() {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\\*").length;
    }
    
    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "UssdRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", text='" + text + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", networkCode='" + networkCode + '\'' +
                '}';
    }
}