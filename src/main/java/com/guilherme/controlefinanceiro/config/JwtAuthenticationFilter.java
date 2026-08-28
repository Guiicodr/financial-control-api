package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extrai e valida o token JWT do header Authorization e popula o
 * SecurityContextHolder.
 *
 * IMPORTANTE: NÃO é um @Component. Ele é instanciado dentro do
 * SecurityFilterChain (SecurityConfig) — se fosse um bean, o Spring Boot o
 * registraria também no servlet container, e a autenticação definida aqui
 * seria descartada pelo SecurityContextHolderFilter da cadeia (causa clássica
 * de 401 logo após login bem-sucedido).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight CORS não precisa de autenticação
        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        return "OPTIONS".equalsIgnoreCase(method) && origin != null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Sem header Bearer: segue anônimo (endpoints públicos passam; protegidos
        // recebem 401 do entry point). Nunca lançar exceção aqui.
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        if (token.isEmpty()) {
            log.debug("Header Authorization presente mas token vazio: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1) Valida assinatura + expiração e extrai o e-mail (uma única passada)
            String email = jwtService.validarEExtrairEmail(token);

            // 2) Só autentica se ainda não houver autenticação no contexto
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 3) Injeta o usuário autenticado no contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (UsernameNotFoundException e) {
            log.warn("Token válido mas usuário não existe mais: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (JwtException e) {
            // Token expirado, assinatura inválida, malformado etc. — 401 limpo,
            // sem stack trace e sem quebrar o fluxo.
            log.info("Token JWT rejeitado em {}: {}", request.getRequestURI(), e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Erro inesperado validando token JWT", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}