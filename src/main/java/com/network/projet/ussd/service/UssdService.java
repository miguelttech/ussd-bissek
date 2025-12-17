package com.network.projet.ussd.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.model.dto.UssdRequest;
import com.network.projet.ussd.model.dto.UssdResponse;
import com.network.projet.ussd.model.dto.UssdState;
import com.network.projet.ussd.model.dto.UssdStateConfig;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Service
public class UssdService {
    
    private static final String INITIAL_STATE = "1";
    
    private UssdStateConfig stateConfig;
    private final ValidationService validationService;
    
    // Stocker l'état actuel par sessionId
    private final Map<String, String> sessionStates = new ConcurrentHashMap<>();
    
    // Stocker les données collectées par session
    private final Map<String, Map<String, String>> sessionData = new ConcurrentHashMap<>();
    
    public UssdService(ValidationService validationService) {
        this.validationService = validationService;
    }
    
    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("ussd_states.json");
        stateConfig = mapper.readValue(resource.getInputStream(), UssdStateConfig.class);
    }
    
    public Mono<UssdResponse> handleRequest(UssdRequest request) {
        return Mono.fromCallable(() -> {
            String sessionId = request.getSessionId();
            
            // 1️⃣ Déterminer l'état courant
            String currentStateId = sessionStates.getOrDefault(sessionId, INITIAL_STATE);
            
            // 2️⃣ Si requête initiale, afficher l'état initial
            if (request.isInitialRequest()) {
                currentStateId = INITIAL_STATE;
                sessionStates.put(sessionId, currentStateId);
                
                UssdState state = stateConfig.getState(currentStateId);
                if (state == null) {
                    throw new IllegalStateException("État initial introuvable: " + currentStateId);
                }
                
                return UssdResponse.continueSession(state.getMessage());
            }
            
            // 3️⃣ Récupérer l'état actuel
            UssdState currentState = stateConfig.getState(currentStateId);
            if (currentState == null) {
                throw new IllegalStateException("État introuvable: " + currentStateId);
            }
            
            // 4️⃣ Récupérer la dernière entrée utilisateur
            String lastInput = request.getLastInput();
            
            // 5️⃣ Déterminer l'état suivant
            String nextStateId = getNextState(currentState, lastInput, sessionId);
            
            if (nextStateId == null) {
                // Entrée invalide → rester sur l'état actuel
                return UssdResponse.continueSession(
                    "Entrée invalide.\n\n" + currentState.getMessage()
                );
            }
            
            // 6️⃣ Stocker la donnée si c'est un état avec validation
            if (currentState.hasValidations()) {
                storeSessionData(sessionId, currentStateId, lastInput);
            }
            
            // 7️⃣ Si état END ou état final, terminer
            if ("END".equals(nextStateId) || isEndState(nextStateId)) {
                sessionStates.remove(sessionId);
                sessionData.remove(sessionId); // Nettoyer les données
                
                UssdState endState = stateConfig.getState(nextStateId);
                if (endState == null) {
                    return UssdResponse.endSession("Merci d'avoir utilisé PickNDrop.");
                }
                return UssdResponse.endSession(endState.getMessage());
            }
            
            // 8️⃣ Mettre à jour l'état de la session
            sessionStates.put(sessionId, nextStateId);
            
            // 9️⃣ Récupérer et afficher l'état suivant
            UssdState nextState = stateConfig.getState(nextStateId);
            if (nextState == null) {
                throw new IllegalStateException("État suivant introuvable: " + nextStateId);
            }
            
            return UssdResponse.continueSession(nextState.getMessage());
        });
    }
    
    /**
     * Détermine l'état suivant en fonction de l'input et des validations
     */
    private String getNextState(UssdState currentState, String input, String sessionId) {
        // 1. Vérifier les options (menus)
        if (currentState.hasOptions() && currentState.getOptions().containsKey(input)) {
            return currentState.getOptions().get(input);
        }
        
        // 2. Vérifier les validations
        if (currentState.hasValidations()) {
            Map<String, String> validations = currentState.getValidations();
            
            // Parcourir toutes les validations
            for (Map.Entry<String, String> validation : validations.entrySet()) {
                String validationType = validation.getKey();
                String nextStateId = validation.getValue();
                
                // Valider l'input
                if (validationService.validate(validationType, input)) {
                    return nextStateId; // Validation réussie
                }
            }
            
            // Si aucune validation ne passe, retourner onInvalid
            return currentState.getOnInvalid();
        }
        
        // 3. Si transition automatique (next)
        if (currentState.hasNext()) {
            return currentState.getNext();
        }
        
        // 4. Si rien ne correspond, retourner onInvalid ou rester sur place
        return currentState.getOnInvalid() != null ? currentState.getOnInvalid() : null;
    }
    
    /**
     * Stocke les données collectées pendant la session
     */
    private void storeSessionData(String sessionId, String stateId, String value) {
        sessionData.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                   .put(stateId, value);
    }
    
    /**
     * Récupère les données d'une session
     */
    public Map<String, String> getSessionData(String sessionId) {
        return sessionData.get(sessionId);
    }
    
    /**
     * Vérifie si un état est un état final
     */
    private boolean isEndState(String stateId) {
        UssdState state = stateConfig.getState(stateId);
        return state != null && state.isFinal();
    }
}