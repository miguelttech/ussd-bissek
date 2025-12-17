package com.network.projet.ussd.model.automata;

public enum TransitionType {
    /**
     * Transition directe (ex: "1" -> ENTER_WEIGHT)
     */
    DIRECT,
    
    /**
     * Transition avec validation (ex: "nomValide" -> état 10)
     */
    VALIDATION,
    
    /**
     * Retour arrière (ex: "99" -> état précédent)
     */
    BACK,
    
    /**
     * Wildcard - accepte n'importe quelle entrée (ex: "*" -> reste sur l'état actuel)
     */
    WILDCARD
}