package com.network.projet.ussd.model.automata;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class State {
    private String id;
    private String message;
    
    @JsonProperty("next")
    private String nextStateId;
    
    @JsonProperty("options")
    private Map<String, String> options;
    
    // Constructeurs
    public State() {}
    
    public State(String id, String message) {
        this.id = id;
        this.message = message;
    }
    
    // Getters et Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getNextStateId() {
        return nextStateId;
    }
    
    public void setNextStateId(String nextStateId) {
        this.nextStateId = nextStateId;
    }
    
    public Map<String, String> getOptions() {
        return options;
    }
    
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
    
    // Méthodes utilitaires
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }
    
    public boolean isFinalState() {
        return "END".equals(id);
    }
    
    public String getNextState(String userInput) {
        if (hasOptions()) {
            return options.get(userInput);
        }
        return nextStateId;
    }
    
    @Override
    public String toString() {
        return "State{id='" + id + "', message='" + message + "'}";
    }
}