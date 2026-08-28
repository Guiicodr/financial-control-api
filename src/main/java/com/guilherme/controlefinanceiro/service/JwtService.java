package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long validadeMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:900000}") long validadeMs) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter pelo menos 32 caracteres (256 bits) para assinatura HS256");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.validadeMs = validadeMs;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(validadeMs)))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida assinatura e expiração em UMA única passada e devolve o e-mail
     * (subject). Lança JwtException (ExpiredJwtException, SignatureException,
     * MalformedJwtException...) se o token for inválido — a exceção é tratada
     * pelo filtro e nunca derruba a requisição com 500.
     */
    public String validarEExtrairEmail(String token) {
        return claims(token).getSubject();
    }

    public String extrairEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean valido(String token, String email) {
        try {
            return email != null && email.equals(claims(token).getSubject());
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                // Tolerância a pequenas diferenças de relógio entre emissão e validação
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}