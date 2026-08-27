package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public Usuario registrar(String name, String email, String senha) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Nome é obrigatório");
        if (usuarios.findByEmail(email).isPresent())
            throw new IllegalArgumentException("E-mail já cadastrado");
        return usuarios.save(new Usuario(name.trim(), email.toLowerCase().trim(), encoder.encode(senha)));
    }

    public Resultado autenticar(String email, String senha) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        return emitir(usuario);
    }

    public Resultado renovar(String token) {
        RefreshToken refresh = refreshTokens.findByToken(token)
                .filter(item -> item.getExpiracao().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido ou expirado"));
        return emitir(refresh.getUsuario());
    }

    private Resultado emitir(Usuario usuario) {
        refreshTokens.deleteByUsuario(usuario);
        RefreshToken refresh = new RefreshToken();
        refresh.setToken(UUID.randomUUID().toString());
        refresh.setUsuario(usuario);
        refresh.setExpiracao(Instant.now().plusSeconds(60L * 60 * 24 * 30));
        refreshTokens.save(refresh);
        return new Resultado(jwtService.gerar(usuario), refresh.getToken(), usuario.getName());
    }

    public record Resultado(String accessToken, String refreshToken, String name) {
    }
}
