package com.network.projet.ussd.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Représente un état USSD du fichier JSON
 */
public class UssdState {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("options")
    private Map<String, String> options;
    
    @JsonProperty("next")
    private String next;
    
    @JsonProperty("validations")
    private Map<String, String> validations;
    
    @JsonProperty("onInvalid")
    private String onInvalid;
    
    @JsonProperty("isFinal")
    private boolean isFinal;
    
    // Constructeurs
    public UssdState() {
    }
    
    public UssdState(String message) {
        this.message = message;
    }
    
    // Getters et Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, String> getOptions() {
        return options;
    }
    
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
    
    public String getNext() {
        return next;
    }
    
    public void setNext(String next) {
        this.next = next;
    }
    
    public Map<String, String> getValidations() {
        return validations;
    }
    
    public void setValidations(Map<String, String> validations) {
        this.validations = validations;
    }
    
    public String getOnInvalid() {
        return onInvalid;
    }
    
    public void setOnInvalid(String onInvalid) {
        this.onInvalid = onInvalid;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }
    
    // Méthodes utilitaires
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
    
    public boolean hasNext() {
        return next != null && !next.isEmpty();
    }
    
    public boolean hasValidations() {
        return validations != null && !validations.isEmpty();
    }
    
    /**
     * Détermine l'état suivant basé sur l'input utilisateur
     */
    public String getNextState(String userInput) {
        // 1. Vérifier les options d'abord
        if (hasOptions() && options.containsKey(userInput)) {
            return options.get(userInput);
        }
        
        // 2. Vérifier les validations (nomValide, emailValide, etc.)
        if (hasValidations()) {
            // Ici tu devras implémenter la logique de validation
            // Pour l'instant, on retourne la première validation
            for (String nextStateId : validations.values()) {
                return nextStateId; // Retourne l'état de validation
            }
        }
        
        // 3. Si rien ne correspond, retourner next ou onInvalid
        if (next != null) {
            return next;
        }
        
        return onInvalid; // Rester sur l'état actuel si input invalide
    }
    
    @Override
    public String toString() {
        return "UssdState{" +
                "message='" + message + '\'' +
                ", options=" + options +
                ", next='" + next + '\'' +
                ", validations=" + validations +
                ", onInvalid='" + onInvalid + '\'' +
                ", isFinal=" + isFinal +
                '}';
    }
}