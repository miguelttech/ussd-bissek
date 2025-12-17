// ============================================
// UssdController.java
// Version WebFlux avec Africa's Talking
// ============================================
package com.network.projet.ussd.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.network.projet.ussd.model.dto.UssdRequest;
import com.network.projet.ussd.model.dto.UssdResponse;
import com.network.projet.ussd.service.UssdService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ussd")
public class UssdController {
    
    private final UssdService ussdService;
    
    public UssdController(UssdService ussdService) {
        this.ussdService = ussdService;
    }
    
    /**
     * Endpoint principal pour Africa's Talking
     * Reçoit le format standard et retourne une simple string
     */
    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> handleUssd(@RequestBody UssdRequest request) {
        return ussdService.handleRequest(request)
            .map(UssdResponse::getResponse)
            .onErrorResume(e -> {
                // En cas d'erreur, terminer avec un message d'erreur
                return Mono.just("END Erreur: " + e.getMessage());
            });
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("USSD Service is running and healthy.");
    }
}