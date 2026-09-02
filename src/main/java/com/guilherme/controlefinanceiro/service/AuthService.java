package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.PasswordResetToken;
import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.PasswordResetTokenRepository;
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

    private final PasswordResetTokenRepository resetTokens;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
            AuthenticationManager authenticationManager, JwtService jwtService,
            PasswordResetTokenRepository resetTokens) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.resetTokens = resetTokens;
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

    @Transactional
    public String solicitarResetSenha(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("E-mail é obrigatório");
        usuarios.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("E-mail não encontrado"));
        resetTokens.deleteByEmail(email.toLowerCase().trim());
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        resetTokens.save(new PasswordResetToken(token, email.toLowerCase().trim(), Instant.now().plusSeconds(3600)));
        log.info("🔐 Reset token gerado para {}: {}", email, token);
        return token;
    }

    @Transactional
    public void resetarSenha(String token, String novaSenha) {
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Token é obrigatório");
        if (novaSenha == null || novaSenha.length() < 6)
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        PasswordResetToken reset = resetTokens.findByTokenAndUtilizadoFalse(token)
                .filter(r -> r.getExpiracao().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));
        Usuario usuario = usuarios.findByEmail(reset.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usuario.setSenha(encoder.encode(novaSenha));
        usuarios.save(usuario);
        reset.setUtilizado(true);
        resetTokens.save(reset);
        log.info("🔐 Senha redefinida para: {}", reset.getEmail());
    }
}