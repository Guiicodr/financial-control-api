package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.PasswordResetToken;
import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.PasswordResetTokenRepository;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.util.PiiMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    /** Em dev é possível ver o token no log para testar o fluxo; em prod, nunca. */
    private final boolean exporTokenEmLog;

    public AuthService(UsuarioRepository usuarios, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
            AuthenticationManager authenticationManager, JwtService jwtService,
            PasswordResetTokenRepository resetTokens,
            @Value("${app.reset-token.log-for-dev:false}") boolean exporTokenEmLog) {
        this.usuarios = usuarios;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.resetTokens = resetTokens;
        this.exporTokenEmLog = exporTokenEmLog;
    }

    public Usuario registrar(String name, String email, String senha, String aceiteVersao) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Nome é obrigatório");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("E-mail é obrigatório");
        if (senha == null || senha.isBlank())
            throw new IllegalArgumentException("Senha é obrigatória");
        // LGPD art. 8, §1º: o aceite dos documentos legais é condição do
        // cadastro. A verificação fica no serviço (e não só no controller) para
        // valer para qualquer chamada futura de registro.
        if (aceiteVersao == null || aceiteVersao.isBlank())
            throw new IllegalArgumentException(
                    "É necessário aceitar os Termos de Uso e o Aviso de Privacidade para criar a conta");
        if (usuarios.findByEmail(email.toLowerCase().trim()).isPresent())
            throw new IllegalArgumentException("E-mail já cadastrado");

        Usuario usuario = new Usuario(name.trim(), email.toLowerCase().trim(), encoder.encode(senha));
        // Versão vem do cliente (é o documento que ele leu); o instante é do
        // servidor, para não depender do relógio de quem se cadastra.
        usuario.setAceiteVersao(aceiteVersao.trim());
        usuario.setAceiteEm(Instant.now());
        return usuarios.save(usuario);
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
    public void solicitarResetSenha(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("E-mail é obrigatório");
        String normalizado = email.toLowerCase().trim();

        // Não revela se o e-mail existe: sem usuário, o método termina em silêncio.
        // Sem isso, o erro "E-mail não encontrado" permitia enumerar contas.
        usuarios.findByEmail(normalizado).ifPresent(usuario -> {
            resetTokens.deleteByEmail(normalizado);
            String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
            resetTokens.save(new PasswordResetToken(token, normalizado, Instant.now().plusSeconds(3600)));
            notificarReset(usuario, token);
        });
    }

    /**
     * Entrega do token ao DONO do e-mail.
     *
     * O token NUNCA é devolvido na resposta HTTP nem escrito no log de produção:
     * enquanto era devolvido, qualquer pessoa que soubesse o e-mail de outra
     * podia pedir o reset e trocar a senha — tomada de conta completa.
     *
     * Pendência consciente: o envio por e-mail ainda não está implementado (exige
     * spring-boot-starter-mail + credenciais SMTP). Até lá, em produção a
     * solicitação apenas é registrada e o usuário precisa de outro canal de
     * recuperação. Em dev (app.reset-token.log-for-dev=true) o token vai para o
     * log para permitir testar o fluxo localmente.
     */
    private void notificarReset(Usuario usuario, String token) {
        if (exporTokenEmLog) {
            log.warn("🔐 [SOMENTE DEV] Reset token de {}: {}", PiiMasker.email(usuario.getEmail()), token);
            return;
        }
        log.info("🔐 Solicitação de recuperação registrada para {} (token válido por 1 hora).",
                PiiMasker.email(usuario.getEmail()));
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
        // Sessões antigas morrem junto com a senha antiga: se a conta foi
        // comprometida, o invasor perde o refresh token de 30 dias.
        refreshTokens.deleteByUsuario(usuario);
        log.info("🔐 Senha redefinida para: {}", PiiMasker.email(reset.getEmail()));
    }
}