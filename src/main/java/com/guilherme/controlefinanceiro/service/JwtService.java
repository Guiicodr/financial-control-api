package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long validadeMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:900000}") long validadeMs) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validadeMs = validadeMs;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder().subject(usuario.getEmail()).issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(validadeMs))).signWith(chave).compact();
    }

    public String extrairEmail(String token) {
        return claims(token).getSubject();
    }

    public boolean valido(String token, String email) {
        return email.equals(extrairEmail(token)) && !claims(token).getExpiration().before(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
    }
}
