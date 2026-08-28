package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Carrega o usuário pelo e-mail. A ausência do usuário é sinalizada com
     * UsernameNotFoundException — exceção prevista no contrato do Spring Security:
     * o DaoAuthenticationProvider a converte em BadCredentialsException (401 limpo),
     * e o JwtAuthenticationFilter captura qualquer RuntimeException. Em nenhum
     * cenário essa exceção derruba o container.
     */
    @Bean
    UserDetailsService userDetailsService(UsuarioRepository repository) {
        return email -> repository.findByEmail(email)
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getSenha())
                        .roles("USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Entry point que responde 401 + JSON em vez do Http403ForbiddenEntryPoint
     * padrão (que devolvia 403 e confundia o frontend).
     */
    private AuthenticationEntryPoint naoAutenticadoEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Não autenticado\"}");
        };
    }

    /**
     * O CorsConfigurationSource é injetado aqui (definido UMA única vez em
     * CorsConfig). O bean duplicado que existia neste arquivo causava
     * BeanDefinitionOverrideException e derrubava o boot no Railway.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight do navegador sempre liberado
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // /auth/** totalmente aberto (registro, login e refresh via POST)
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        // Webhook do WhatsApp: autenticado pelo token do provedor,
                        // não por JWT (o remetente é o WhatsApp/Twilio/Meta).
                        .requestMatchers("/webhooks/**").permitAll()
                        // CRÍTICO: o Boot despacha erros (404/405/500) para /error e
                        // esse dispatch passa pela cadeia de segurança. Sem este
                        // permitAll, TODO erro não tratado virava 401 "Não autenticado".
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(naoAutenticadoEntryPoint()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Filtro instanciado aqui (não é bean): evita o registro duplo
                // servlet container + security chain que descartava a autenticação.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}