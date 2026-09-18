package com.guilherme.controlefinanceiro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
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

    /**
     * Origens de produção do front, lista EXPLÍCITA e fechada.
     *
     * Sem wildcard de propósito: o padrão antigo ("https://*.vercel.app")
     * confiava em QUALQUER site publicado na Vercel por qualquer pessoa,
     * que passava a poder ler as respostas autenticadas da API.
     *
     * Estão aqui as duas URLs que o projeto realmente usa na Vercel — o alias
     * canônico e a URL de deployment. Faltava a segunda, e como
     * CORS_ALLOWED_ORIGINS não vinha definida no Railway, toda chamada do front
     * publicado era recusada com 403 e o app não funcionava em produção
     * (o teste loginPermitePostDaOrigemVercel existe exatamente para pegar isso).
     *
     * Domínio próprio no futuro: definir CORS_ALLOWED_ORIGINS com a lista
     * definitiva e estas constantes deixam de ser usadas.
     */
    private static final List<String> PRODUCAO_PATTERNS = List.of(
            "https://financial-control-dashboard.vercel.app",
            "https://financial-control-dashboard-smoky.vercel.app");

    /** Só em desenvolvimento: front rodando no Vite local. */
    private static final List<String> DEV_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*");

    /**
     * Escape hatch para testar preview deploys (URLs geradas por branch).
     * Habilitar apenas temporariamente via CORS_ALLOW_VERCEL_PREVIEWS=true,
     * porque reabre a confiança em qualquer subdomínio da Vercel.
     */
    private static final String VERCEL_PREVIEW_PATTERN = "https://*.vercel.app";

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOriginsEnv,
            @Value("${app.cors.allow-vercel-previews:false}") boolean permitirPreviews,
            Environment environment) {

        List<String> patterns = new ArrayList<>();
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        boolean override = allowedOriginsEnv != null && !allowedOriginsEnv.isBlank();

        if (override) {
            // Produção: lista explícita definida em CORS_ALLOWED_ORIGINS.
            patterns.addAll(Arrays.stream(allowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList());
        } else {
            // Sem env var: usamos a lista exata do projeto (nunca wildcard).
            patterns.addAll(PRODUCAO_PATTERNS);
        }

        if (permitirPreviews) {
            patterns.add(VERCEL_PREVIEW_PATTERN);
        }

        if (dev) {
            patterns.addAll(DEV_PATTERNS);
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(patterns);
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