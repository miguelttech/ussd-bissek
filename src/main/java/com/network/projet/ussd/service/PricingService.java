package com.network.projet.ussd.service;

import com.network.projet.ussd.model.enums.DeliveryType;
import com.network.projet.ussd.model.enums.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for pricing calculations.
 * Calculates shipment costs based on multiple factors.
 * 
 * <p>Pricing Formula:
 * <pre>
 * Base Price = (Weight × Weight Rate) + Distance Rate
 * Transport Multiplier = Based on transport mode
 * Delivery Multiplier = Based on delivery speed
 * Special Handling Fee = If fragile/perishable/liquid
 * 
 * TOTAL = (Base Price × Transport × Delivery) + Special Fee
 * </pre>
 * 
 * <p>Example Calculation:
 * <pre>
 * Weight: 5 kg
 * Transport: MOTORCYCLE (multiplier 1.0)
 * Delivery: EXPRESS_24H (multiplier 2.0)
 * Special: Yes (fee 1000 XAF)
 * 
 * Base = (5 × 500) + 1000 = 3500 XAF
 * Total = (3500 × 1.0 × 2.0) + 1000 = 8000 XAF
 * </pre>
 * 
 * @author Thomas Djotio Ndié
 * @version 1.0
 * @since 2025-01-15
 */
@Service
@Slf4j
public class PricingService {
    
    // ============================================
    // CONSTANTES DE TARIFICATION
    // ============================================
    
    /**
     * Base rate per kilogram (XAF).
     * Applied to package weight.
     */
    private static final BigDecimal BASE_RATE_PER_KG = new BigDecimal("500");
    
    /**
     * Base distance rate (XAF).
     * Flat rate for local delivery.
     */
    private static final BigDecimal BASE_DISTANCE_RATE = new BigDecimal("1000");
    
    /**
     * Special handling fee (XAF).
     * Added for fragile, perishable, or liquid items.
     */
    private static final BigDecimal SPECIAL_HANDLING_FEE = new BigDecimal("1000");
    
    /**
     * Insurance rate (percentage).
     * Applied to declared value.
     */
    private static final BigDecimal INSURANCE_RATE = new BigDecimal("0.02"); // 2%
    
    /**
     * Minimum insurance fee (XAF).
     */
    private static final BigDecimal MIN_INSURANCE_FEE = new BigDecimal("500");
    
    // ============================================
    // MÉTHODES DE CALCUL
    // ============================================
    
    /**
     * Calculates total shipment price.
     * 
     * <p>This method is reactive (returns Mono) even though calculation
     * is synchronous, to maintain consistency with service layer architecture.
     * In future, this could call external pricing APIs reactively.
     * 
     * @param weight package weight in kg
     * @param transport_mode transport method
     * @param delivery_type delivery speed
     * @param special_handling requires special handling
     * @return Mono<BigDecimal> total price in XAF
     */
    public Mono<BigDecimal> calculate_price(
            BigDecimal weight,
            TransportMode transport_mode,
            DeliveryType delivery_type,
            boolean special_handling) {
        
        log.debug("Calculating price for weight={}, transport={}, delivery={}, special={}",
            weight, transport_mode, delivery_type, special_handling);
        
        return Mono.fromCallable(() -> {
            // ÉTAPE 1: Prix de base (poids + distance)
            BigDecimal base_price = calculate_base_price(weight);
            
            // ÉTAPE 2: Multiplicateur transport
            BigDecimal transport_multiplier = get_transport_multiplier(transport_mode);
            
            // ÉTAPE 3: Multiplicateur livraison
            BigDecimal delivery_multiplier = get_delivery_multiplier(delivery_type);
            
            // ÉTAPE 4: Calculer le prix avec multiplicateurs
            BigDecimal price_with_multipliers = base_price
                .multiply(transport_multiplier)
                .multiply(delivery_multiplier);
            
            // ÉTAPE 5: Ajouter frais spéciaux si nécessaire
            BigDecimal total_price = price_with_multipliers;
            if (special_handling) {
                total_price = total_price.add(SPECIAL_HANDLING_FEE);
            }
            
            // ÉTAPE 6: Arrondir à 2 décimales
            total_price = total_price.setScale(2, RoundingMode.HALF_UP);
            
            log.info("Calculated total price: {} XAF", total_price);
            return total_price;
        });
    }
    
    /**
     * Calculates base price from weight.
     * Formula: (weight × rate_per_kg) + base_distance
     * 
     * @param weight package weight
     * @return base price
     */
    private BigDecimal calculate_base_price(BigDecimal weight) {
        BigDecimal weight_cost = weight.multiply(BASE_RATE_PER_KG);
        return weight_cost.add(BASE_DISTANCE_RATE);
    }
    
    /**
     * Gets transport mode price multiplier.
     * Different transport modes have different costs.
     * 
     * @param transport_mode transport method
     * @return multiplier (e.g., 1.5 = 150% of base)
     */
    private BigDecimal get_transport_multiplier(TransportMode transport_mode) {
        return switch (transport_mode) {
            case BICYCLE -> new BigDecimal("0.8");      // 80% - Cheapest
            case MOTORCYCLE -> new BigDecimal("1.0");   // 100% - Standard
            case TRICYCLE -> new BigDecimal("1.2");     // 120%
            case CAR -> new BigDecimal("1.5");          // 150%
            case TRUCK -> new BigDecimal("2.0");        // 200% - Most expensive
        };
    }
    
    /**
     * Gets delivery type price multiplier.
     * Faster delivery = higher cost.
     * 
     * @param delivery_type delivery speed
     * @return multiplier
     */
    private BigDecimal get_delivery_multiplier(DeliveryType delivery_type) {
        return switch (delivery_type) {
            case STANDARD -> new BigDecimal("1.0");      // 100% - Normal
            case EXPRESS_48H -> new BigDecimal("1.5");   // 150%
            case EXPRESS_24H -> new BigDecimal("2.0");   // 200% - Fastest
        };
    }
    
    /**
     * Calculates insurance cost.
     * 
     * @param declared_value package declared value
     * @return Mono<BigDecimal> insurance cost
     */
    public Mono<BigDecimal> calculate_insurance(BigDecimal declared_value) {
        return Mono.fromCallable(() -> {
            if (declared_value == null || declared_value.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            
            // Calculer 2% de la valeur
            BigDecimal insurance_cost = declared_value.multiply(INSURANCE_RATE);
            
            // Minimum 500 XAF
            if (insurance_cost.compareTo(MIN_INSURANCE_FEE) < 0) {
                insurance_cost = MIN_INSURANCE_FEE;
            }
            
            return insurance_cost.setScale(2, RoundingMode.HALF_UP);
        });
    }
    
    /**
     * Gets price estimate without saving.
     * Used for USSD quote display.
     * 
     * @param weight package weight
     * @param transport_mode transport method
     * @param delivery_type delivery speed
     * @param special_handling requires special handling
     * @param insured is insured
     * @param declared_value declared value if insured
     * @return Mono<PriceEstimate> detailed price breakdown
     */
    public Mono<PriceEstimate> get_price_estimate(
            BigDecimal weight,
            TransportMode transport_mode,
            DeliveryType delivery_type,
            boolean special_handling,
            boolean insured,
            BigDecimal declared_value) {
        
        return calculate_price(weight, transport_mode, delivery_type, special_handling)
            .flatMap(base_price -> {
                if (insured) {
                    return calculate_insurance(declared_value)
                        .map(insurance_cost -> {
                            BigDecimal total = base_price.add(insurance_cost);
                            return PriceEstimate.builder()
                                .base_price(base_price)
                                .insurance_cost(insurance_cost)
                                .total_price(total)
                                .build();
                        });
                } else {
                    return Mono.just(PriceEstimate.builder()
                        .base_price(base_price)
                        .insurance_cost(BigDecimal.ZERO)
                        .total_price(base_price)
                        .build());
                }
            });
    }
    
    /**
     * Inner class for price estimates.
     */
    @lombok.Builder
    @lombok.Data
    public static class PriceEstimate {
        private BigDecimal base_price;
        private BigDecimal insurance_cost;
        private BigDecimal total_price;
        
        /**
         * Gets formatted total price.
         * 
         * @return formatted string with currency
         */
        public String getFormattedTotal() {
            return String.format("%,.0f XAF", total_price);
        }
        
        /**
         * Gets formatted breakdown.
         * 
         * @return multi-line breakdown
         */
        public String getFormattedBreakdown() {
            StringBuilder breakdown = new StringBuilder();
            breakdown.append(String.format("Prix de base: %,.0f XAF\n", base_price));
            
            if (insurance_cost.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.append(String.format("Assurance: %,.0f XAF\n", insurance_cost));
            }
            
            breakdown.append(String.format("TOTAL: %,.0f XAF", total_price));
            return breakdown.toString();
        }
    }
}