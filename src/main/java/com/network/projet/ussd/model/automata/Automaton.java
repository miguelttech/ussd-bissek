package com.network.projet.ussd.model.automata;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Automaton {
    private Map<String, State> states;
    private String initialStateId;
    private State currentState;
    
    public Automaton() {
        this.states = new HashMap<>();
    }
    
    /**
     * Initialise l'automate à partir d'un fichier JSON
     */
    public void loadFromJson(String jsonFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(jsonFilePath);
        
        if (inputStream == null) {
            throw new IOException("Fichier JSON introuvable: " + jsonFilePath);
        }
        
        // Parse le JSON en Map<String, State>
        Map<String, State> loadedStates = mapper.readValue(
            inputStream,
            mapper.getTypeFactory().constructMapType(
                HashMap.class, String.class, State.class
            )
        );
        
        // Assigne l'ID à chaque état
        loadedStates.forEach((key, state) -> {
            state.setId(key);
            this.states.put(key, state);
        });
        
        // Définit l'état initial (MAIN_MENU par défaut)
        this.initialStateId = "MAIN_MENU";
        this.currentState = states.get(initialStateId);
        
        System.out.println("Automate chargé avec " + states.size() + " états");
    }
    
    /**
     * Initialise l'automate directement depuis une String JSON
     */
    public void loadFromJsonString(String jsonContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        
        Map<String, State> loadedStates = mapper.readValue(
            jsonContent,
            mapper.getTypeFactory().constructMapType(
                HashMap.class, String.class, State.class
            )
        );
        
        loadedStates.forEach((key, state) -> {
            state.setId(key);
            this.states.put(key, state);
        });
        
        this.initialStateId = "MAIN_MENU";
        this.currentState = states.get(initialStateId);
    }
    
    /**
     * Réinitialise l'automate à l'état initial
     */
    public void reset() {
        this.currentState = states.get(initialStateId);
    }
    
    /**
     * Récupère un état par son ID
     */
    public State getState(String stateId) {
        State state = states.get(stateId);
        if (state == null) {
            throw new IllegalArgumentException("État introuvable: " + stateId);
        }
        return state;
    }
    
    /**
     * Traite l'entrée utilisateur et retourne le prochain état
     */
    public State processInput(String currentStateId, String userInput) {
        State current = getState(currentStateId);
        String nextStateId = current.getNextState(userInput);
        
        if (nextStateId == null) {
            throw new IllegalArgumentException(
                "Transition invalide depuis " + currentStateId + 
                " avec l'entrée: " + userInput
            );
        }
        
        State nextState = getState(nextStateId);
        this.currentState = nextState;
        return nextState;
    }
    
    /**
     * Vérifie si une entrée est valide pour l'état actuel
     */
    public boolean isValidInput(String stateId, String userInput) {
        State state = getState(stateId);
        
        if (state.hasOptions()) {
            return state.getOptions().containsKey(userInput);
        }
        
        // Si pas d'options, n'importe quelle entrée non-vide est valide
        return userInput != null && !userInput.trim().isEmpty();
    }
    
    // Getters
    public Map<String, State> getStates() {
        return states;
    }
    
    public String getInitialStateId() {
        return initialStateId;
    }
    
    public State getCurrentState() {
        return currentState;
    }
    
    public State getInitialState() {
        return states.get(initialStateId);
    }
}