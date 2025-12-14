package com.network.projet.ussd.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ussd")
public class UssdSimulatorController {

    // Stockage temporaire des sessions en mémoire
    private final Map<String, UssdSession> sessions = new ConcurrentHashMap<>();

    /* TESTS SEULEMENT */
    @GetMapping
    public String test() {
        return "USSD Simulator is running. Use POST to /api/ussd with JSON body.";
    }

    // Endpoint principal pour simuler USSD sur /api/ussd
    @PostMapping
    public Map<String, String> simulate(@RequestBody Map<String, String> payload) {
        String sessionId = payload.get("sessionId");
        String msisdn = payload.get("msisdn");
        String input = payload.get("input");

        // Récupérer ou créer la session
        UssdSession session = sessions.computeIfAbsent(sessionId, k -> new UssdSession(sessionId, msisdn));

        String responseMessage;
        String type = "CON"; // par défaut, la session continue

        // Logique simple basée sur l'état actuel
        switch (session.getCurrentState()) {

            case MAIN_MENU -> {
                switch (input) {
                    case "1" -> {
                        session.setCurrentState(UssdState.ENTER_WEIGHT);
                        responseMessage = "CON Entrez le poids du colis (en kg) :";
                    }
                    case "2" -> {
                        session.setCurrentState(UssdState.TRACK_PACKAGE);
                        responseMessage = "CON Entrez le numéro du colis à suivre :";
                    }
                    case "0" -> {
                        responseMessage = "END Merci d'avoir utilisé PickNDrop.";
                        type = "END";
                        sessions.remove(sessionId);
                    }
                    default -> responseMessage = "CON Option invalide. 1: Envoyer 2: Suivre 0: Quitter";
                }
            }

            case ENTER_WEIGHT -> {
                session.getAnswers().put("weight", input);
                session.setCurrentState(UssdState.ENTER_ADDRESS);
                responseMessage = "CON Entrez l'adresse de livraison :";
            }

            case ENTER_ADDRESS -> {
                session.getAnswers().put("address", input);
                session.setCurrentState(UssdState.CONFIRMATION);
                String weight = session.getAnswers().get("weight");
                String address = session.getAnswers().get("address");
                responseMessage = "CON Confirmez l'envoi :\nPoids: " + weight + " kg\nAdresse: " + address + "\n1: Confirmer 0: Annuler";
            }

            case CONFIRMATION -> {
                switch (input) {
                    case "1" -> {
                        responseMessage = "END Colis enregistré avec succès. Merci d'utiliser PickNDrop.";
                        type = "END";
                        sessions.remove(sessionId);
                    }
                    case "0" -> {
                        responseMessage = "END Envoi annulé.";
                        type = "END";
                        sessions.remove(sessionId);
                    }
                    default -> responseMessage = "CON Option invalide. 1: Confirmer 0: Annuler";
                }
            }

            case TRACK_PACKAGE -> {
                responseMessage = "END Le colis #" + input + " est en cours de livraison.";
                type = "END";
                sessions.remove(sessionId);
            }

            default -> {
                responseMessage = "END Erreur. Session réinitialisée.";
                type = "END";
                sessions.remove(sessionId);
            }
        }

        return Map.of(
                "type", type,
                "message", responseMessage
        );
    }

    // ======= Classes internes pour la simulation =======

    // États de l'automate
    private enum UssdState {
        MAIN_MENU,
        ENTER_WEIGHT,
        ENTER_ADDRESS,
        CONFIRMATION,
        TRACK_PACKAGE
    }

    // Session utilisateur USSD
    private static class UssdSession {
        private final String sessionId;
        private final String msisdn;
        private UssdState currentState;
        private final Map<String, String> answers;

        public UssdSession(String sessionId, String msisdn) {
            this.sessionId = sessionId;
            this.msisdn = msisdn;
            this.currentState = UssdState.MAIN_MENU;
            this.answers = new HashMap<>();
        }

        public UssdState getCurrentState() {
            return currentState;
        }

        public void setCurrentState(UssdState currentState) {
            this.currentState = currentState;
        }

        public Map<String, String> getAnswers() {
            return answers;
        }
    }
}
