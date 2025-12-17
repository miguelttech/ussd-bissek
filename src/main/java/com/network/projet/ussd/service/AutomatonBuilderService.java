// ============================================
// AutomatonBuilderService.java
// Service for building automaton from JSON configuration
// ============================================
package com.network.projet.ussd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.network.projet.ussd.model.automata.Automaton;
import com.network.projet.ussd.model.automata.State;
import com.network.projet.ussd.model.automata.Transition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for building automaton from JSON configuration.
 * This service is the bridge between declarative JSON and in-memory automaton model.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Load JSON configuration from classpath</li>
 *   <li>Parse JSON structure</li>
 *   <li>Build State objects</li>
 *   <li>Build Transition objects</li>
 *   <li>Validate automaton integrity</li>
 *   <li>Cache built automaton</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * Automaton automaton = automatonBuilderService.getAutomaton().block();
 * State currentState = automaton.getState("STATE_1");
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatonBuilderService {
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    /**
     * Cached automaton instance.
     * Built once at startup, reused for all requests.
     */
    private Automaton cachedAutomaton;
    
    /**
     * Path to automaton configuration file.
     * Can be overridden via application.properties:
     * ussd.automaton.config-path=classpath:automaton-config.json
     */
    private static final String DEFAULT_CONFIG_PATH = "classpath:ussd-automaton-config.json";
    
    /**
     * Initializes automaton at application startup.
     * Fails fast if configuration is invalid.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing USSD automaton from configuration...");
        
        try {
            cachedAutomaton = buildAutomatonFromJson(DEFAULT_CONFIG_PATH).block();
            
            if (cachedAutomaton != null) {
                log.info("Automaton initialized successfully: {}", cachedAutomaton);
                
                // Validate automaton
                List<String> validationErrors = cachedAutomaton.validate();
                if (!validationErrors.isEmpty()) {
                    log.error("Automaton validation errors: {}", validationErrors);
                    throw new IllegalStateException("Invalid automaton configuration: " + validationErrors);
                }
                
                // Log statistics
                Map<String, Object> stats = cachedAutomaton.getStatistics();
                log.info("Automaton statistics: {}", stats);
            } else {
                throw new IllegalStateException("Failed to build automaton from configuration");
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize automaton", e);
            throw new RuntimeException("Cannot start application without valid automaton configuration", e);
        }
    }
    
    /**
     * Gets the cached automaton instance.
     * This is the main method called by other services.
     * 
     * @return Mono with automaton
     */
    public Mono<Automaton> getAutomaton() {
        if (cachedAutomaton == null) {
            return Mono.error(new IllegalStateException("Automaton not initialized"));
        }
        return Mono.just(cachedAutomaton);
    }
    
    /**
     * Builds automaton from JSON configuration file.
     * 
     * @param configPath path to JSON file
     * @return Mono with built automaton
     */
    public Mono<Automaton> buildAutomatonFromJson(String configPath) {
        return Mono.fromCallable(() -> {
            log.info("Loading automaton configuration from: {}", configPath);
            
            // Load JSON file
            Resource resource = resourceLoader.getResource(configPath);
            if (!resource.exists()) {
                throw new IOException("Configuration file not found: " + configPath);
            }
            
            // Parse JSON
            JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
            
            // Build automaton
            return parseAutomatonFromJson(rootNode);
        });
    }
    
    /**
     * Parses automaton structure from JSON node.
     * 
     * @param rootNode root JSON node
     * @return built automaton
     */
    private Automaton parseAutomatonFromJson(JsonNode rootNode) {
        log.debug("Parsing automaton from JSON...");
        
        Automaton automaton = Automaton.builder()
            .automatonId(rootNode.get("automatonId").asText())
            .name(rootNode.get("name").asText())
            .version(rootNode.get("version").asText())
            .description(rootNode.has("description") ? rootNode.get("description").asText() : null)
            .initialStateId(rootNode.get("initialStateId").asText())
            .states(new HashMap<>())
            .transitions(new ArrayList<>())
            .finalStateIds(new HashSet<>())
            .metadata(new HashMap<>())
            .build();
        
        // Parse metadata if present
        if (rootNode.has("metadata")) {
            JsonNode metadataNode = rootNode.get("metadata");
            metadataNode.fields().forEachRemaining(entry -> 
                automaton.getMetadata().put(entry.getKey(), entry.getValue().asText()));
        }
        
        // Parse states
        JsonNode statesNode = rootNode.get("states");
        if (statesNode != null && statesNode.isArray()) {
            statesNode.forEach(stateNode -> {
                State state = parseState(stateNode);
                automaton.addState(state);
            });
            log.info("Parsed {} states", automaton.getStates().size());
        }
        
        // Parse transitions
        JsonNode transitionsNode = rootNode.get("transitions");
        if (transitionsNode != null && transitionsNode.isArray()) {
            transitionsNode.forEach(transitionNode -> {
                Transition transition = parseTransition(transitionNode);
                automaton.addTransition(transition);
            });
            log.info("Parsed {} transitions", automaton.getTransitions().size());
        }
        
        // Parse final state IDs
        JsonNode finalStatesNode = rootNode.get("finalStateIds");
        if (finalStatesNode != null && finalStatesNode.isArray()) {
            finalStatesNode.forEach(node -> automaton.getFinalStateIds().add(node.asText()));
        }
        
        log.debug("Automaton parsing complete");
        return automaton;
    }
    
    /**
     * Parses a single state from JSON node.
     * 
     * @param stateNode state JSON node
     * @return built state
     */
    private State parseState(JsonNode stateNode) {
        State.StateBuilder builder = State.builder()
            .stateId(stateNode.get("stateId").asText())
            .label(stateNode.get("label").asText())
            .stateType(State.StateType.valueOf(stateNode.get("stateType").asText()))
            .displayMessage(stateNode.get("displayMessage").asText())
            .description(getTextOrNull(stateNode, "description"))
            .validationType(getTextOrNull(stateNode, "validationType"))
            .businessServiceMethod(getTextOrNull(stateNode, "businessServiceMethod"))
            .contextStorageKey(getTextOrNull(stateNode, "contextStorageKey"))
            .requiresContext(getBooleanOrFalse(stateNode, "requiresContext"))
            .terminatesSession(getBooleanOrFalse(stateNode, "terminatesSession"));
        
        // Parse menu options if present
        if (stateNode.has("menuOptions") && stateNode.get("menuOptions").isArray()) {
            List<State.MenuOption> menuOptions = new ArrayList<>();
            stateNode.get("menuOptions").forEach(optionNode -> {
                State.MenuOption option = State.MenuOption.builder()
                    .optionKey(optionNode.get("optionKey").asText())
                    .optionText(optionNode.get("optionText").asText())
                    .transitionTrigger(optionNode.get("transitionTrigger").asText())
                    .icon(getTextOrNull(optionNode, "icon"))
                    .build();
                menuOptions.add(option);
            });
            builder.menuOptions(menuOptions);
        }
        
        // Parse required context keys if present
        if (stateNode.has("requiredContextKeys") && stateNode.get("requiredContextKeys").isArray()) {
            List<String> requiredKeys = new ArrayList<>();
            stateNode.get("requiredContextKeys").forEach(node -> requiredKeys.add(node.asText()));
            builder.requiredContextKeys(requiredKeys);
        }
        
        return builder.build();
    }
    
    /**
     * Parses a single transition from JSON node.
     * 
     * @param transitionNode transition JSON node
     * @return built transition
     */
    private Transition parseTransition(JsonNode transitionNode) {
        Transition.TransitionBuilder builder = Transition.builder()
            .fromStateId(transitionNode.get("fromStateId").asText())
            .toStateId(transitionNode.get("toStateId").asText())
            .trigger(transitionNode.get("trigger").asText())
            .label(getTextOrNull(transitionNode, "label"))
            .description(getTextOrNull(transitionNode, "description"))
            .priority(getIntOrDefault(transitionNode, "priority", 0))
            .requiresValidation(getBooleanOrFalse(transitionNode, "requiresValidation"))
            .validationType(getTextOrNull(transitionNode, "validationType"))
            .isErrorTransition(getBooleanOrFalse(transitionNode, "isErrorTransition"))
            .isFallbackTransition(getBooleanOrFalse(transitionNode, "isFallbackTransition"))
            .errorMessage(getTextOrNull(transitionNode, "errorMessage"))
            .maxRetries(getIntOrDefault(transitionNode, "maxRetries", 3));
        
        // Parse guard conditions if present
        if (transitionNode.has("guardConditions") && transitionNode.get("guardConditions").isArray()) {
            List<String> guards = new ArrayList<>();
            transitionNode.get("guardConditions").forEach(node -> guards.add(node.asText()));
            builder.guardConditions(guards);
        }
        
        // Parse actions if present
        if (transitionNode.has("actions") && transitionNode.get("actions").isArray()) {
            List<String> actions = new ArrayList<>();
            transitionNode.get("actions").forEach(node -> actions.add(node.asText()));
            builder.actions(actions);
        }
        
        return builder.build();
    }
    
    // ============================================
    // UTILITY METHODS
    // ============================================
    
    /**
     * Safely gets text value from JSON node or returns null.
     */
    private String getTextOrNull(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }
    
    /**
     * Safely gets boolean value from JSON node or returns false.
     */
    private boolean getBooleanOrFalse(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asBoolean();
        }
        return false;
    }
    
    /**
     * Safely gets int value from JSON node or returns default.
     */
    private int getIntOrDefault(JsonNode node, String fieldName, int defaultValue) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asInt();
        }
        return defaultValue;
    }
    
    /**
     * Reloads automaton from configuration.
     * Can be called at runtime to hot-reload configuration.
     * 
     * @return Mono with reloaded automaton
     */
    public Mono<Automaton> reloadAutomaton() {
        log.info("Reloading automaton configuration...");
        
        return buildAutomatonFromJson(DEFAULT_CONFIG_PATH)
            .doOnSuccess(automaton -> {
                cachedAutomaton = automaton;
                log.info("Automaton reloaded successfully");
            })
            .doOnError(error -> log.error("Failed to reload automaton", error));
    }
}