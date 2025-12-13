package com.network.projet.ussd.exception;

/**
 * Exception thrown when an invalid transition is attempted
 */
public class InvalidTransitionException extends RuntimeException {
    
    private String fromStateId;
    private String toStateId;
    private String trigger;
    
    public InvalidTransitionException(String message) {
        super(message);
    }
    
    public InvalidTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InvalidTransitionException(String message, String fromStateId, String toStateId) {
        super(message);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
    }
    
    public InvalidTransitionException(String message, String fromStateId, 
                                     String toStateId, String trigger) {
        super(message);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.trigger = trigger;
    }
    
    public InvalidTransitionException(String message, String fromStateId, 
                                     String toStateId, String trigger, Throwable cause) {
        super(message, cause);
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.trigger = trigger;
    }
    
    public String getFromStateId() {
        return fromStateId;
    }
    
    public String getToStateId() {
        return toStateId;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (fromStateId != null && toStateId != null) {
            sb.append(" [").append(fromStateId).append(" -> ").append(toStateId);
            if (trigger != null) {
                sb.append(" (").append(trigger).append(")");
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
