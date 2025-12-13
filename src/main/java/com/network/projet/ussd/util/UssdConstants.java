package com.network.projet.ussd.util;

/**
 * Constants used throughout the USSD application
 */
public class UssdConstants {
    
    // Session constants
    public static final long SESSION_TIMEOUT_MINUTES = 10;
    public static final String SESSION_ID_PREFIX = "USSD_";
    
    // Menu constants
    public static final String MAIN_MENU_ID = "MAIN_MENU";
    public static final String PREVIOUS_OPTION = "0";
    public static final String EXIT_OPTION = "*";
    
    // Response constants
    public static final String RESPONSE_END = "END";
    public static final String RESPONSE_CONTINUE = "CON";
    
    // Validation constants
    public static final int MIN_PHONE_LENGTH = 7;
    public static final int MAX_PHONE_LENGTH = 15;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 50;
    
    // Database constants
    public static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String DB_URL = "jdbc:mysql://localhost:3306/ussd_db";
    
    // Error messages
    public static final String ERROR_INVALID_STATE = "Invalid state";
    public static final String ERROR_INVALID_TRANSITION = "Invalid transition";
    public static final String ERROR_SESSION_EXPIRED = "Session has expired";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    
    // Message keys
    public static final String MSG_WELCOME = "welcome.message";
    public static final String MSG_INVALID_INPUT = "error.invalid.input";
    public static final String MSG_PLEASE_SELECT = "prompt.select";
    
    // Encoding constants
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    // State names
    public static final String STATE_WELCOME = "WELCOME";
    public static final String STATE_MAIN = "MAIN";
    public static final String STATE_INPUT = "INPUT";
    public static final String STATE_CONFIRMATION = "CONFIRMATION";
    public static final String STATE_SUCCESS = "SUCCESS";
    public static final String STATE_ERROR = "ERROR";
    
    private UssdConstants() {
        // Private constructor to prevent instantiation
    }
}
