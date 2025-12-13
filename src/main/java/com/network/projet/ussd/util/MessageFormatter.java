package com.network.projet.ussd.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting USSD messages and responses
 */
public class MessageFormatter {
    
    private static final String LINE_SEPARATOR = "\n";
    private static final String OPTION_PREFIX = ". ";
    private static final int MAX_MESSAGE_LENGTH = 160;
    
    private MessageFormatter() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Formats a menu response
     */
    public static String formatMenu(String title, String... options) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(LINE_SEPARATOR);
        
        for (int i = 0; i < options.length; i++) {
            sb.append(i + 1).append(OPTION_PREFIX).append(options[i]);
            if (i < options.length - 1) {
                sb.append(LINE_SEPARATOR);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a continuation response
     */
    public static String formatContinue(String message) {
        return UssdConstants.RESPONSE_CONTINUE + " " + truncateMessage(message);
    }
    
    /**
     * Formats an end response
     */
    public static String formatEnd(String message) {
        return UssdConstants.RESPONSE_END + " " + truncateMessage(message);
    }
    
    /**
     * Formats an error message
     */
    public static String formatError(String errorMessage) {
        return formatEnd("Error: " + errorMessage);
    }
    
    /**
     * Truncates message to maximum length
     */
    public static String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }
        return message;
    }
    
    /**
     * Formats a timestamp
     */
    public static String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
    
    /**
     * Formats currency
     */
    public static String formatCurrency(double amount, String currency) {
        return String.format("%s %.2f", currency, amount);
    }
    
    /**
     * Formats a phone number
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        if (cleaned.length() >= 10) {
            return cleaned.substring(0, cleaned.length() - 4) + "****";
        }
        return cleaned;
    }
    
    /**
     * Checks if message fits in USSD limit
     */
    public static boolean isWithinLimit(String message) {
        return message != null && message.length() <= MAX_MESSAGE_LENGTH;
    }
    
    /**
     * Formats key-value pairs for display
     */
    public static String formatKeyValue(String key, String value) {
        return key + ": " + value;
    }
}
