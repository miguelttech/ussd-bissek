package com.network.projet.ussd.service;

import com.network.projet.ussd.exception.InvalidStateException;
import com.network.projet.ussd.exception.SessionExpiredException;
import com.network.projet.ussd.model.entities.*;
import com.network.projet.ussd.model.enums.*;
import com.network.projet.ussd.util.MessageFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Main USSD service orchestrating the complete flow.
 * This is the "brain" of the USSD application.
 * 
 * <p>Architecture:
 * <pre>
 * Africa's Talking → Controller → UssdService → Other Services
 *                                      ↓
 *                              State Machine Logic
 *                                      ↓
 *                              Response Builder
 * </pre>
 * 
 * <p>Flow Example:
 * <pre>
 * 1. User dials *384*96#
 * 2. UssdService checks if session exists
 * 3. If new: Create session, show welcome
 * 4. If existing: Get current state, process input
 * 5. Validate input for current state
 * 6. Transition to next state
 * 7. Build and return response
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UssdService {
    
    private final SessionService session_service;
    private final UserService user_service;
    private final ShipmentService shipment_service;
    private final ValidationService validation_service;
    private final PricingService pricing_service;
    
    /**
     * Main entry point for USSD requests.
     * 
     * <p>Request Flow:
     * 1. Check if session exists
     * 2. If new: Initialize session
     * 3. If existing: Get state and process
     * 4. Return formatted response
     * 
     * @param session_id session identifier from Africa's Talking
     * @param phone_number user's phone number
     * @param user_input user's text input (empty on first call)
     * @return Mono<UssdResponse> formatted USSD response
     */
    public Mono<UssdResponse> process_ussd_request(
            String session_id, 
            String phone_number, 
            String user_input) {
        
        log.info("Processing USSD request - Session: {}, Phone: {}, Input: {}", 
            session_id, phone_number, user_input);
        
        // Vérifier si c'est une nouvelle session
        return session_service.is_valid_session(session_id)
            .flatMap(is_valid -> {
                if (!is_valid) {
                    // Nouvelle session : afficher le menu d'accueil
                    return handle_new_session(session_id, phone_number);
                } else {
                    // Session existante : traiter l'input
                    return handle_existing_session(session_id, phone_number, user_input);
                }
            })
            .onErrorResume(SessionExpiredException.class, e -> {
                // Session expirée : recréer
                log.warn("Session expired, creating new one");
                return handle_new_session(session_id, phone_number);
            })
            .onErrorResume(throwable -> {
                // Erreur générique
                log.error("Error processing USSD request", throwable);
                return Mono.just(UssdResponse.end(
                    "Erreur système. Veuillez réessayer."));
            });
    }
    
    /**
     * Handles new session initialization.
     * Shows welcome screen and main menu.
     * 
     * @param session_id session identifier
     * @param phone_number user's phone
     * @return Mono<UssdResponse> welcome message
     */
    private Mono<UssdResponse> handle_new_session(String session_id, String phone_number) {
        log.info("Initializing new session for: {}", phone_number);
        
        return session_service.create_session(phone_number)
            .then(user_service.find_by_phone(phone_number))
            .flatMap(user -> {
                // Utilisateur existant : menu principal personnalisé
                return session_service.update_session(session_id, "user_id", user.getId())
                    .then(session_service.update_state(session_id, "MAIN_MENU"))
                    .thenReturn(build_main_menu_response(user.getName()));
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Nouvel utilisateur : menu d'inscription
                return session_service.update_state(session_id, "REGISTER_MENU")
                    .thenReturn(build_register_menu_response());
            }));
    }
    
    /**
     * Handles existing session with user input.
     * 
     * @param session_id session identifier
     * @param phone_number user's phone
     * @param user_input user's input
     * @return Mono<UssdResponse> next screen
     */
    private Mono<UssdResponse> handle_existing_session(
            String session_id, 
            String phone_number, 
            String user_input) {
        
        return session_service.get_session(session_id)
            .flatMap(session_data -> {
                String current_state = (String) session_data.get("current_state");
                
                log.debug("Current state: {}, Input: {}", current_state, user_input);
                
                // Router selon l'état actuel
                return route_to_state_handler(session_id, current_state, user_input, session_data);
            });
    }
    
    /**
     * Routes to appropriate state handler based on current state.
     * This implements the state machine logic from automate.txt
     * 
     * @param session_id session identifier
     * @param current_state current state ID
     * @param user_input user's input
     * @param session_data session data map
     * @return Mono<UssdResponse> next response
     */
    private Mono<UssdResponse> route_to_state_handler(
            String session_id,
            String current_state,
            String user_input,
            Map<String, Object> session_data) {
        
        return switch (current_state) {
            case "MAIN_MENU" -> handle_main_menu(session_id, user_input);
            case "REGISTER_MENU" -> handle_register_menu(session_id, user_input);
            
            // Flux d'envoi de colis
            case "SEND_PACKAGE" -> handle_send_package_start(session_id, user_input);
            case "ENTER_RECIPIENT_NAME" -> handle_recipient_name(session_id, user_input);
            case "ENTER_RECIPIENT_PHONE" -> handle_recipient_phone(session_id, user_input);
            case "ENTER_RECIPIENT_EMAIL" -> handle_recipient_email(session_id, user_input);
            case "ENTER_RECIPIENT_CITY" -> handle_recipient_city(session_id, user_input);
            case "ENTER_RECIPIENT_ADDRESS" -> handle_recipient_address(session_id, user_input);
            case "ENTER_PACKAGE_DESCRIPTION" -> handle_package_description(session_id, user_input);
            case "ENTER_PACKAGE_WEIGHT" -> handle_package_weight(session_id, user_input);
            case "ENTER_FRAGILE" -> handle_fragile(session_id, user_input);
            case "ENTER_PERISHABLE" -> handle_perishable(session_id, user_input);
            case "ENTER_LIQUID" -> handle_liquid(session_id, user_input);
            case "ENTER_INSURED" -> handle_insured(session_id, user_input);
            case "ENTER_DECLARED_VALUE" -> handle_declared_value(session_id, user_input);
            case "SELECT_TRANSPORT" -> handle_transport_selection(session_id, user_input);
            case "SELECT_DELIVERY_TYPE" -> handle_delivery_type(session_id, user_input);
            case "SELECT_PAYMENT" -> handle_payment_method(session_id, user_input);
            case "CONFIRM_SHIPMENT" -> handle_shipment_confirmation(session_id, user_input);
            
            // Flux de suivi
            case "TRACK_PACKAGE" -> handle_track_package(session_id, user_input);
            
            default -> Mono.just(UssdResponse.end(
                "État invalide. Session terminée."));
        };
    }
    
    // ============================================
    // MENU BUILDERS
    // ============================================
    
    private UssdResponse build_main_menu_response(String user_name) {
        String message = MessageFormatter.formatMenu(
            "Bienvenue " + user_name + "\nMenu Principal:",
            "Envoyer un colis",
            "Suivre un colis",
            "Mes envois",
            "Mon compte"
        );
        return UssdResponse.continueSession(message);
    }
    
    private UssdResponse build_register_menu_response() {
        String message = MessageFormatter.formatMenu(
            "Bienvenue chez PickNDrop!\nVeuillez vous inscrire:",
            "Client",
            "Livreur Freelance",
            "Agence",
            "Retour"
        );
        return UssdResponse.continueSession(message);
    }
    
    // ============================================
    // STATE HANDLERS - MAIN MENU
    // ============================================
    
    private Mono<UssdResponse> handle_main_menu(String session_id, String input) {
        return switch (input.trim()) {
            case "1" -> {
                // Envoyer un colis
                yield session_service.update_state(session_id, "ENTER_RECIPIENT_NAME")
                    .thenReturn(UssdResponse.continueSession(
                        "Entrez le nom complet du destinataire:"));
            }
            case "2" -> {
                // Suivre un colis
                yield session_service.update_state(session_id, "TRACK_PACKAGE")
                    .thenReturn(UssdResponse.continueSession(
                        "Entrez le numéro de suivi (ex: PKND-20250115-00001):"));
            }
            case "3" -> handle_my_shipments(session_id);
            case "4" -> handle_my_account(session_id);
            default -> Mono.just(UssdResponse.continueSession(
                "Option invalide. Veuillez choisir 1-4."));
        };
    }
    
    // ============================================
    // STATE HANDLERS - SEND PACKAGE FLOW
    // ============================================
    
    private Mono<UssdResponse> handle_recipient_name(String session_id, String name) {
        return validation_service.validate_name(name)
            .flatMap(valid_name -> 
                session_service.store_answer(session_id, "recipient_name", valid_name)
                    .then(session_service.update_state(session_id, "ENTER_RECIPIENT_PHONE"))
                    .thenReturn(UssdResponse.continueSession(
                        "Entrez le téléphone du destinataire (ex: +237600000000):")))
            .onErrorResume(throwable -> 
                Mono.just(UssdResponse.continueSession(
                    "Nom invalide. " + throwable.getMessage() + "\nRéessayez:")));
    }
    
    private Mono<UssdResponse> handle_recipient_phone(String session_id, String phone) {
        return validation_service.validate_phone(phone)
            .flatMap(valid_phone ->
                session_service.store_answer(session_id, "recipient_phone", valid_phone)
                    .then(session_service.update_state(session_id, "ENTER_RECIPIENT_EMAIL"))
                    .thenReturn(UssdResponse.continueSession(
                        "Email du destinataire (optionnel, tapez 0 pour ignorer):")))
            .onErrorResume(throwable ->
                Mono.just(UssdResponse.continueSession(
                    "Téléphone invalide. Réessayez:")));
    }
    
    private Mono<UssdResponse> handle_recipient_email(String session_id, String email) {
        if ("0".equals(email.trim())) {
            return session_service.update_state(session_id, "ENTER_RECIPIENT_CITY")
                .thenReturn(UssdResponse.continueSession(
                    "Entrez la ville de livraison:"));
        }
        
        return validation_service.validate_email(email)
            .flatMap(valid_email ->
                session_service.store_answer(session_id, "recipient_email", valid_email)
                    .then(session_service.update_state(session_id, "ENTER_RECIPIENT_CITY"))
                    .thenReturn(UssdResponse.continueSession(
                        "Entrez la ville de livraison:")))
            .onErrorResume(throwable ->
                Mono.just(UssdResponse.continueSession(
                    "Email invalide. Réessayez ou tapez 0 pour ignorer:")));
    }
    
    private Mono<UssdResponse> handle_recipient_city(String session_id, String city) {
        return validation_service.validate_city(city)
            .flatMap(valid_city ->
                session_service.store_answer(session_id, "recipient_city", valid_city)
                    .then(session_service.update_state(session_id, "ENTER_RECIPIENT_ADDRESS"))
                    .thenReturn(UssdResponse.continueSession(
                        "Entrez l'adresse complète de livraison:")))
            .onErrorResume(throwable ->
                Mono.just(UssdResponse.continueSession(
                    "Ville invalide. Réessayez:")));
    }
    
    private Mono<UssdResponse> handle_recipient_address(String session_id, String address) {
        return validation_service.validate_address(address)
            .flatMap(valid_address ->
                session_service.store_answer(session_id, "recipient_address", valid_address)
                    .then(session_service.update_state(session_id, "ENTER_PACKAGE_DESCRIPTION"))
                    .thenReturn(UssdResponse.continueSession(
                        "Description du colis:")))
            .onErrorResume(throwable ->
                Mono.just(UssdResponse.continueSession(
                    "Adresse invalide. Réessayez:")));
    }
    
    private Mono<UssdResponse> handle_package_description(String session_id, String description) {
        if (description.trim().length() < 5) {
            return Mono.just(UssdResponse.continueSession(
                "Description trop courte (min 5 caractères). Réessayez:"));
        }
        
        return session_service.store_answer(session_id, "package_description", description)
            .then(session_service.update_state(session_id, "ENTER_PACKAGE_WEIGHT"))
            .thenReturn(UssdResponse.continueSession(
                "Poids du colis en kg (ex: 5.5):"));
    }
    
    private Mono<UssdResponse> handle_package_weight(String session_id, String weight) {
        return validation_service.validate_weight(weight)
            .flatMap(valid_weight ->
                session_service.store_answer(session_id, "package_weight", valid_weight)
                    .then(session_service.update_state(session_id, "ENTER_FRAGILE"))
                    .thenReturn(UssdResponse.continueSession(
                        "Colis fragile?\n1. Oui\n2. Non")))
            .onErrorResume(throwable ->
                Mono.just(UssdResponse.continueSession(
                    "Poids invalide (0.5-500 kg). Réessayez:")));
    }
    
    private Mono<UssdResponse> handle_fragile(String session_id, String input) {
        String fragile = "1".equals(input.trim()) ? "true" : "false";
        
        return session_service.store_answer(session_id, "fragile", fragile)
            .then(session_service.update_state(session_id, "ENTER_PERISHABLE"))
            .thenReturn(UssdResponse.continueSession(
                "Denrée périssable?\n1. Oui\n2. Non"));
    }
    
    private Mono<UssdResponse> handle_perishable(String session_id, String input) {
        String perishable = "1".equals(input.trim()) ? "true" : "false";
        
        return session_service.store_answer(session_id, "perishable", perishable)
            .then(session_service.update_state(session_id, "ENTER_LIQUID"))
            .thenReturn(UssdResponse.continueSession(
                "Contient du liquide?\n1. Oui\n2. Non"));
    }
    
    private Mono<UssdResponse> handle_liquid(String session_id, String input) {
        String liquid = "1".equals(input.trim()) ? "true" : "false";
        
        return session_service.store_answer(session_id, "liquid", liquid)
            .then(session_service.update_state(session_id, "ENTER_INSURED"))
            .thenReturn(UssdResponse.continueSession(
                "Assurer le colis?\n1. Oui\n2. Non"));
    }
    
    private Mono<UssdResponse> handle_insured(String session_id, String input) {
        if ("1".equals(input.trim())) {
            return session_service.store_answer(session_id, "insured", "true")
                .then(session_service.update_state(session_id, "ENTER_DECLARED_VALUE"))
                .thenReturn(UssdResponse.continueSession(
                    "Valeur déclarée en XAF:"));
        } else {
            return session_service.store_answer(session_id, "insured", "false")
                .then(session_service.update_state(session_id, "SELECT_TRANSPORT"))
                .thenReturn(build_transport_menu());
        }
    }
    
    private Mono<UssdResponse> handle_declared_value(String session_id, String value) {
        try {
            BigDecimal declared_value = new BigDecimal(value);
            if (declared_value.compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.just(UssdResponse.continueSession(
                    "Valeur doit être positive. Réessayez:"));
            }
            
            return session_service.store_answer(session_id, "declared_value", value)
                .then(session_service.update_state(session_id, "SELECT_TRANSPORT"))
                .thenReturn(build_transport_menu());
                
        } catch (NumberFormatException e) {
            return Mono.just(UssdResponse.continueSession(
                "Valeur invalide. Réessayez:"));
        }
    }
    
    private UssdResponse build_transport_menu() {
        String message = MessageFormatter.formatMenu(
            "Moyen de transport:",
            "Camion",
            "Tricycle",
            "Moto",
            "Vélo",
            "Voiture"
        );
        return UssdResponse.continueSession(message);
    }
    
    private Mono<UssdResponse> handle_transport_selection(String session_id, String input) {
        TransportMode transport = switch (input.trim()) {
            case "1" -> TransportMode.TRUCK;
            case "2" -> TransportMode.TRICYCLE;
            case "3" -> TransportMode.MOTORCYCLE;
            case "4" -> TransportMode.BICYCLE;
            case "5" -> TransportMode.CAR;
            default -> null;
        };
        
        if (transport == null) {
            return Mono.just(UssdResponse.continueSession(
                "Option invalide. Choisissez 1-5."));
        }
        
        return session_service.store_answer(session_id, "transport", transport.name())
            .then(session_service.update_state(session_id, "SELECT_DELIVERY_TYPE"))
            .thenReturn(build_delivery_type_menu());
    }
    
    private UssdResponse build_delivery_type_menu() {
        String message = MessageFormatter.formatMenu(
            "Type de livraison:",
            "Standard (3 jours)",
            "Express 48h",
            "Express 24h"
        );
        return UssdResponse.continueSession(message);
    }
    
    private Mono<UssdResponse> handle_delivery_type(String session_id, String input) {
        DeliveryType delivery_type = switch (input.trim()) {
            case "1" -> DeliveryType.STANDARD;
            case "2" -> DeliveryType.EXPRESS_48H;
            case "3" -> DeliveryType.EXPRESS_24H;
            default -> null;
        };
        
        if (delivery_type == null) {
            return Mono.just(UssdResponse.continueSession(
                "Option invalide. Choisissez 1-3."));
        }
        
        return session_service.store_answer(session_id, "delivery_type", delivery_type.name())
            .then(session_service.update_state(session_id, "SELECT_PAYMENT"))
            .thenReturn(build_payment_menu());
    }
    
    private UssdResponse build_payment_menu() {
        String message = MessageFormatter.formatMenu(
            "Mode de paiement:",
            "Espèces",
            "Mobile Money",
            "Orange Money",
            "Payé par destinataire"
        );
        return UssdResponse.continueSession(message);
    }
    
    private Mono<UssdResponse> handle_payment_method(String session_id, String input) {
        PaymentMethod payment = switch (input.trim()) {
            case "1" -> PaymentMethod.CASH;
            case "2" -> PaymentMethod.MOBILE_MONEY;
            case "3" -> PaymentMethod.ORANGE_MONEY;
            case "4" -> PaymentMethod.PAID_BY_RECIPIENT;
            default -> null;
        };
        
        if (payment == null) {
            return Mono.just(UssdResponse.continueSession(
                "Option invalide. Choisissez 1-4."));
        }
        
        return session_service.store_answer(session_id, "payment_method", payment.name())
            .then(show_confirmation_summary(session_id));
    }
    
    private Mono<UssdResponse> show_confirmation_summary(String session_id) {
        return session_service.get_all_answers(session_id)
            .flatMap(answers -> {
                // Calculer le prix
                BigDecimal weight = new BigDecimal(answers.get("package_weight"));
                TransportMode transport = TransportMode.valueOf(answers.get("transport"));
                DeliveryType delivery = DeliveryType.valueOf(answers.get("delivery_type"));
                boolean special = Boolean.parseBoolean(answers.get("fragile")) ||
                                Boolean.parseBoolean(answers.get("perishable")) ||
                                Boolean.parseBoolean(answers.get("liquid"));
                
                return pricing_service.calculate_price(weight, transport, delivery, special)
                    .map(price -> {
                        String summary = String.format(
                            "RÉCAPITULATIF:\n" +
                            "Destinataire: %s\n" +
                            "Ville: %s\n" +
                            "Poids: %s kg\n" +
                            "Transport: %s\n" +
                            "Livraison: %s\n" +
                            "Prix: %,.0f XAF\n\n" +
                            "1. Confirmer\n" +
                            "2. Annuler",
                            answers.get("recipient_name"),
                            answers.get("recipient_city"),
                            answers.get("package_weight"),
                            transport.getDisplayName(),
                            delivery.getDisplayName(),
                            price
                        );
                        
                        return UssdResponse.continueSession(summary);
                    });
            })
            .flatMap(response -> 
                session_service.update_state(session_id, "CONFIRM_SHIPMENT")
                    .thenReturn(response));
    }
    
    private Mono<UssdResponse> handle_shipment_confirmation(String session_id, String input) {
        if (!"1".equals(input.trim())) {
            return session_service.end_session(session_id)
                .thenReturn(UssdResponse.end("Envoi annulé."));
        }
        
        // Créer l'envoi
        return create_shipment_from_session(session_id)
            .flatMap(shipment -> 
                session_service.end_session(session_id)
                    .thenReturn(UssdResponse.end(
                        String.format("Envoi créé avec succès!\n" +
                            "Numéro de suivi: %s\n" +
                            "Merci d'utiliser PickNDrop!",
                            shipment.getTracking_id()))));
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    private Mono<Shipment> create_shipment_from_session(String session_id) {
        return session_service.get_session(session_id)
            .flatMap(session_data -> {
                Long sender_id = ((Number) session_data.get("user_id")).longValue();
                
                return session_service.get_all_answers(session_id)
                    .flatMap(answers -> {
                        // Construire recipient
                        Recipient recipient = Recipient.builder()
                            .name(answers.get("recipient_name"))
                            .phone(answers.get("recipient_phone"))
                            .email(answers.getOrDefault("recipient_email", null))
                            .city(answers.get("recipient_city"))
                            .address(answers.get("recipient_address"))
                            .build();
                        
                        // Construire package
                        Package pkg = Package.builder()
                            .description(answers.get("package_description"))
                            .weight(new BigDecimal(answers.get("package_weight")))
                            .fragile(Boolean.parseBoolean(answers.get("fragile")))
                            .perishable(Boolean.parseBoolean(answers.get("perishable")))
                            .liquid(Boolean.parseBoolean(answers.get("liquid")))
                            .insured(Boolean.parseBoolean(answers.get("insured")))
                            .declared_value(answers.containsKey("declared_value") ?
                                new BigDecimal(answers.get("declared_value")) : null)
                            .build();
                        
                        TransportMode transport = TransportMode.valueOf(answers.get("transport"));
                        DeliveryType delivery = DeliveryType.valueOf(answers.get("delivery_type"));
                        PaymentMethod payment = PaymentMethod.valueOf(answers.get("payment_method"));
                        
                        return shipment_service.create_shipment(
                            sender_id, recipient, pkg, transport, delivery, payment);
                    });
            });
    }
    
    private Mono<UssdResponse> handle_track_package(String session_id, String tracking_id) {
        return shipment_service.track_shipment(tracking_id)
            .map(shipment -> UssdResponse.end(
                String.format("Suivi colis %s:\n" +
                    "Statut: %s\n" +
                    "Destination: %s\n" +
                    "Prix: %,.0f XAF",
                    tracking_id,
                    shipment.getStatus().getDisplayName(),
                    shipment.getDelivery_address(),
                    shipment.getTotal_price())))
            .switchIfEmpty(Mono.just(UssdResponse.end(
                "Colis non trouvé. Vérifiez le numéro.")))
            .flatMap(response -> 
                session_service.end_session(session_id).thenReturn(response));
    }
    
    private Mono<UssdResponse> handle_my_shipments(String session_id) {
        return session_service.get_session(session_id)
            .flatMap(session_data -> {
                Long user_id = ((Number) session_data.get("user_id")).longValue();
                
                return shipment_service.get_recent_shipments(user_id, 5)
                    .collectList()
                    .map(shipments -> {
                        if (shipments.isEmpty()) {
                            return UssdResponse.end("Aucun envoi trouvé.");
                        }
                        
                        StringBuilder message = new StringBuilder("Vos derniers envois:\n\n");
                        for (int i = 0; i < shipments.size(); i++) {
                            Shipment s = shipments.get(i);
                            message.append(String.format("%d. %s - %s\n",
                                i + 1,
                                s.getTracking_id(),
                                s.getStatus().getDisplayName()));
                        }
                        
                        return UssdResponse.end(message.toString());
                    });
            })
            .flatMap(response -> 
                session_service.end_session(session_id).thenReturn(response));
    }
    
    private Mono<UssdResponse> handle_my_account(String session_id) {
        return session_service.get_session(session_id)
            .flatMap(session_data -> {
                Long user_id = ((Number) session_data.get("user_id")).longValue();
                
                return user_service.find_by_id(user_id)
                    .zipWith(shipment_service.get_statistics(user_id))
                    .map(tuple -> {
                        User user = tuple.getT1();
                        ShipmentService.ShipmentStatistics stats = tuple.getT2();
                        
                        String account_info = String.format(
                            "MON COMPTE\n" +
                            "Nom: %s\n" +
                            "Tél: %s\n" +
                            "Type: %s\n\n" +
                            "STATISTIQUES\n" +
                            "En attente: %d\n" +
                            "En transit: %d\n" +
                            "Livrés: %d\n" +
                            "Total: %d",
                            user.getName(),
                            user.getMaskedPhone(),
                            user.getRole().getDisplayName(),
                            stats.getPending_count(),
                            stats.getIn_transit_count(),
                            stats.getDelivered_count(),
                            stats.getTotal_count()
                        );
                        
                        return UssdResponse.end(account_info);
                    });
            })
            .flatMap(response -> 
                session_service.end_session(session_id).thenReturn(response));
    }
    
    private Mono<UssdResponse> handle_register_menu(String session_id, String input) {
        UserType role = switch (input.trim()) {
            case "1" -> UserType.CLIENT;
            case "2" -> UserType.FREELANCE;
            case "3" -> UserType.AGENCY;
            default -> null;
        };
        
        if (role == null) {
            return Mono.just(UssdResponse.continueSession(
                "Option invalide. Choisissez 1-3."));
        }
        
        return session_service.store_answer(session_id, "user_role", role.name())
            .then(session_service.update_state(session_id, "REGISTER_ENTER_NAME"))
            .thenReturn(UssdResponse.continueSession(
                "Entrez votre nom complet:"));
    }
    
    // ============================================
    // RESPONSE DTO
    // ============================================
    
    /**
     * USSD Response DTO.
     * Formats responses according to Africa's Talking requirements.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UssdResponse {
        private String type;    // "CON" or "END"
        private String message; // Response text
        
        /**
         * Creates a continue response (user can respond).
         */
        public static UssdResponse continueSession(String message) {
            return new UssdResponse("CON", message);
        }
        
        /**
         * Creates an end response (session terminates).
         */
        public static UssdResponse end(String message) {
            return new UssdResponse("END", message);
        }
        
        /**
         * Formats for Africa's Talking.
         * Returns: "CON message" or "END message"
         */
        public String format() {
            return type + " " + message;
        }
    }
}