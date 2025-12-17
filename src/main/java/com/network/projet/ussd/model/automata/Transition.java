package com.network.projet.ussd.model.automata;

public class Transition {
    private String condition;
    private String targetStateId;
    private TransitionType type;
    
    public Transition() {}
    
    public Transition(String condition, String targetStateId) {
        this.condition = condition;
        this.targetStateId = targetStateId;
        this.type = determineType(condition);
    }
    
    public Transition(String condition, String targetStateId, TransitionType type) {
        this.condition = condition;
        this.targetStateId = targetStateId;
        this.type = type;
    }
    
    /**
     * Détermine automatiquement le type de transition selon la condition
     */
    private TransitionType determineType(String condition) {
        if (condition == null || condition.isEmpty()) {
            return TransitionType.DIRECT;
        }
        
        if (condition.equals("*")) {
            return TransitionType.WILDCARD;
        }
        
        if (condition.equals("99")) {
            return TransitionType.BACK;
        }
        
        if (condition.matches("\\d+")) {
            // C'est un nombre simple (1, 2, 3...)
            return TransitionType.DIRECT;
        }
        
        if (condition.endsWith("Valide") || condition.contains("Valid")) {
            // nomValide, emailValide, etc.
            return TransitionType.VALIDATION;
        }
        
        return TransitionType.DIRECT;
    }
    
    /**
     * Vérifie si cette transition correspond à l'input utilisateur
     */
    public boolean matches(String userInput) {
        return switch (type) {
            case DIRECT -> condition.equals(userInput);
            case WILDCARD -> true;
            case BACK -> "99".equals(userInput);
            case VALIDATION -> true;
            default -> false;
        }; // Le wildcard match n'importe quoi
        // La validation sera gérée par le service
        // Pour l'instant on retourne true si c'est le type validation
    }
    
    // Getters et Setters
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
        this.type = determineType(condition);
    }
    
    public String getTargetStateId() {
        return targetStateId;
    }
    
    public void setTargetStateId(String targetStateId) {
        this.targetStateId = targetStateId;
    }
    
    public TransitionType getType() {
        return type;
    }
    
    public void setType(TransitionType type) {
        this.type = type;
    }
    
    @Override
    public String toString() {
        return "Transition{" +
                "condition='" + condition + '\'' +
                ", targetStateId='" + targetStateId + '\'' +
                ", type=" + type +
                '}';
    }
}