package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("E-mail é obrigatório");
        if (senha == null || senha.isBlank())
            throw new IllegalArgumentException("Senha é obrigatória");
        if (usuarios.findByEmail(email.toLowerCase().trim()).isPresent())
            throw new IllegalArgumentException("E-mail já cadastrado");
        return usuarios.save(new Usuario(name.trim(), email.toLowerCase().trim(), encoder.encode(senha)));
    }

    @Transactional
    public Resultado autenticar(String email, String senha) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank())
            throw new IllegalArgumentException("E-mail e senha são obrigatórios");

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
        } catch (Exception e) {
            // Falha de credenciais é esperada e tratada: log enxuto, sem stack trace,
            // e reconvertida em IllegalArgumentException (HTTP 400 limpo no controller).
            log.info("Falha de autenticação para o e-mail informado: {}", e.getClass().getSimpleName());
            throw new IllegalArgumentException("E-mail ou senha inválidos");
        }

        // Após autenticação bem-sucedida o usuário existe; qualquer ausência é
        // sinalizada com UsernameNotFoundException (exceção prevista pelo Spring
        // Security), nunca com NoSuchElementException sem mensagem.
        Usuario usuario = usuarios.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return emitir(usuario);
    }

    @Transactional
    public Resultado renovar(String token) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Refresh token é obrigatório");
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