package com.network.projet.ussd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration centrale de l'automate USSD PicknDrop
 * 
 * Active les fonctionnalités réactives nécessaires :
 * - WebFlux pour les endpoints USSD (automatiquement via spring-boot-starter-webflux)
 * - R2DBC pour la persistance réactive avec PostgreSQL
 * - Gestion transactionnelle réactive
 * - Audit automatique des entités (createdAt, updatedAt)
 * - Serving des fichiers statiques (HTML, CSS, JS)
 * 
 * La configuration de la connexion R2DBC et du pool se fait
 * automatiquement via application.properties
 */
@Configuration
@EnableTransactionManagement
@EnableR2dbcAuditing
public class AutomataConfiguration {
    
}