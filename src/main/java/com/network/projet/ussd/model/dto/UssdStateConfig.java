package com.network.projet.ussd.model.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration pour charger tous les états USSD depuis le JSON
 */
public class UssdStateConfig {
    
    private Map<String, UssdState> states = new HashMap<>();
    
    @JsonAnyGetter
    public Map<String, UssdState> getStates() {
        return states;
    }
    
    @JsonAnySetter
    public void setState(String stateId, UssdState state) {
        this.states.put(stateId, state);
    }
    
    public UssdState getState(String stateId) {
        return states.get(stateId);
    }
    
    public boolean hasState(String stateId) {
        return states.containsKey(stateId);
    }
    
    @Override
    public String toString() {
        return "UssdStateConfig{states=" + states.keySet() + "}";
    }
}