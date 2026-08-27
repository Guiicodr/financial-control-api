package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository repository) {
        return email -> repository.findByEmail(email)
                .map(user -> User.withUsername(user.getEmail()).password(user.getSenha()).roles("USER").build())
                .orElseThrow();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Essencial para APIs baseadas em Token (JWT): desativa criação de sessão HTTP
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Libera requisições OPTIONS (Preflight do navegador)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Libera rotas públicas de autenticação e H2 console
                        .requestMatchers("/auth/**", "/h2-console/**").permitAll()
                        // Qualquer outra rota exige autenticação via JWT
                        .anyRequest().authenticated())
                // Necessário caso utilize o console H2 em algum momento
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Insere o filtro JWT antes do filtro de autenticação padrão do Spring
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}