package com.guilherme.controlefinanceiro;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.guilherme.controlefinanceiro.model.Categoria;
import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.service.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de regressão dos achados P0/P1 da auditoria de segurança.
 *
 * Cada teste falha justamente no cenário que existia antes da correção, para
 * que uma futura refatoração não reintroduza:
 * <ul>
 *   <li>S1 — hash BCrypt da senha saindo em listagens (Usuario @JsonIgnore);</li>
 *   <li>S2 — forgot-password devolvendo o token no corpo e enumerando usuários;</li>
 *   <li>S3 — IDOR por id enviado no corpo (Transacao @JsonProperty READ_ONLY);</li>
 *   <li>S6 — brute force em /auth/login sem rate limit;</li>
 *   <li>E3 — índices compostos das consultas por usuário.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private TransacaoRepository transacoes;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    /** Vem do application.properties — mantém o teste alinhado com a configuração real. */
    @Value("${app.rate-limit.auth-requests:20}")
    private int limiteAuth;

    @BeforeEach
    void limparDados() {
        refreshTokens.deleteAll();
        transacoes.deleteAll();
        usuarios.deleteAll();
    }

    private Usuario criarUsuario(String nome, String email) {
        return usuarios.save(new Usuario(nome, email, encoder.encode("senha-segura-123")));
    }

    private String tokenDe(Usuario usuario) {
        return "Bearer " + jwtService.gerar(usuario);
    }

    private Transacao criarTransacao(Usuario dono, String descricao, double valor) {
        Transacao transacao = new Transacao();
        transacao.setDescricao(descricao);
        transacao.setTipo("SAIDA");
        transacao.setValor(valor);
        transacao.setData(LocalDate.now());
        transacao.setCategoria(Categoria.OUTROS);
        transacao.setUsuario(dono);
        return transacoes.save(transacao);
    }

    /** S1 — o dono é serializado junto da transação: nunca pode levar senha/telefone. */
    @Test
    void listagemNaoExpoeSenhaNemTelefoneDoUsuario() throws Exception {
        Usuario usuario = criarUsuario("Ana", "ana@example.com");
        usuario.setTelefone("5511999999999");
        usuario.setSenha(encoder.encode("hash-que-nao-pode-vazar"));
        usuarios.save(usuario);
        criarTransacao(usuario, "Almoco", 25.5);

        String corpo = mockMvc.perform(get("/transacoes")
                .header(HttpHeaders.AUTHORIZATION, tokenDe(usuario)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(corpo.contains("senha"), "GET /transacoes vazou o hash da senha: " + corpo);
        assertFalse(corpo.contains("telefone"), "GET /transacoes vazou o telefone (PII): " + corpo);
        assertTrue(corpo.contains("Almo"), "A transação criada não veio na resposta: " + corpo);
    }

    /** S3 — id no corpo não pode virar UPDATE no registro de outro usuário (IDOR). */
    @Test
    void idNoCorpoNaoSobrescreveTransacaoDeOutroUsuario() throws Exception {
        Usuario dono = criarUsuario("Dono", "dono@example.com");
        Usuario atacante = criarUsuario("Atacante", "atacante@example.com");
        Long idVitima = criarTransacao(dono, "Compra da vitima", 1234.56).getId();

        mockMvc.perform(post("/transacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, tokenDe(atacante))
                .content("{\"id\":" + idVitima
                        + ",\"descricao\":\"invadida\",\"tipo\":\"ENTRADA\",\"valor\":9999.99,"
                        + "\"data\":\"" + LocalDate.now() + "\",\"categoria\":\"OUTROS\"}"))
                .andExpect(status().isOk());

        Transacao depois = transacoes.findById(idVitima).orElseThrow();
        assertEquals(1234.56, depois.getValor(), 0.001, "O valor da vítima foi alterado");
        assertEquals("Compra da vitima", depois.getDescricao(), "A descrição da vítima foi alterada");
        assertEquals(dono.getId(), depois.getUsuario().getId(), "O dono do lançamento mudou");
        assertEquals(2, transacoes.count(),
                "O POST com id no corpo deveria criar um novo lançamento, não reutilizar o id da vítima");
    }

    /** S6 — /auth/login precisa de rate limit por IP (brute force de senhas). */
    @Test
    void loginEhBloqueadoAposExcederLimiteDeTentativas() throws Exception {
        String ip = "203.0.113.44";

        for (int tentativa = 0; tentativa < limiteAuth; tentativa++) {
            mockMvc.perform(post("/auth/login")
                    .header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"alvo@example.com\",\"senha\":\"chute-" + tentativa + "\"}"));
        }

        mockMvc.perform(post("/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alvo@example.com\",\"senha\":\"chute-final\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    /** S2 — a resposta de recuperação de senha é sempre a mesma e sem token. */
    @Test
    void forgotPasswordNaoDevolveTokenNemConfirmaSeOEmailExiste() throws Exception {
        criarUsuario("Ana", "ana@example.com");

        String respostaCadastrado = mockMvc.perform(post("/auth/forgot-password")
                .header("X-Forwarded-For", "203.0.113.61")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String respostaInexistente = mockMvc.perform(post("/auth/forgot-password")
                .header("X-Forwarded-For", "203.0.113.62")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ninguem@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(respostaCadastrado, respostaInexistente,
                "A resposta permite enumerar quais e-mails estão cadastrados");
        assertFalse(respostaCadastrado.toLowerCase(Locale.ROOT).contains("token"),
                "A resposta devolveu o token de recuperação: " + respostaCadastrado);
    }

    /** E3 — índices compostos que sustentam as listagens filtradas por usuário. */
    @Test
    void indicesDeConsultaForamCriados() {
        List<String> encontrados = jdbc.queryForList(
                "select index_name from information_schema.indexes where upper(index_name) like 'IDX_%'",
                String.class).stream()
                .map(nome -> nome.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());

        List<String> esperados = List.of(
                "IDX_TRANSACAO_USUARIO_DATA",
                "IDX_TRANSACAO_USUARIO_CATEGORIA",
                "IDX_INCOME_USUARIO_DATA",
                "IDX_OBJETIVO_USUARIO",
                "IDX_CARTAO_USUARIO",
                "IDX_MOVIMENTO_USUARIO",
                "IDX_MOVIMENTO_OBJETIVO");

        for (String esperado : esperados) {
            assertTrue(encontrados.contains(esperado),
                    "Índice " + esperado + " não foi criado. Encontrados: " + encontrados);
        }
    }
}
