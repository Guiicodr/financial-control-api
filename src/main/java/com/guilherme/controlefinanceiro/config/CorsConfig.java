package com.guilherme.controlefinanceiro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Fonte ÚNICA de configuração CORS da aplicação.
 *
 * O bean duplicado que existia em SecurityConfig.java causava
 * BeanDefinitionOverrideException no boot e derrubava o container no Railway.
 * Este é agora o único lugar onde o CorsConfigurationSource é definido.
 */
@Configuration
public class CorsConfig {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://financial-control-dashboard-smoky.vercel.app");

    /**
     * Permite sobrescrever os origins via variável de ambiente no Railway:
     * CORS_ALLOWED_ORIGINS=https://meu-front.vercel.app,https://outro-front.vercel.app
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOriginsEnv) {

        List<String> origins = (allowedOriginsEnv == null || allowedOriginsEnv.isBlank())
                ? DEFAULT_ALLOWED_ORIGINS
                : Arrays.stream(allowedOriginsEnv.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        // A API usa JWT no header Authorization, não cookies.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}