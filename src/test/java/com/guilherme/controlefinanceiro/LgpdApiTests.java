package com.guilherme.controlefinanceiro;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.guilherme.controlefinanceiro.config.TokenCleanupJob;
import com.guilherme.controlefinanceiro.model.CartaoCredito;
import com.guilherme.controlefinanceiro.model.Categoria;
import com.guilherme.controlefinanceiro.model.PasswordResetToken;
import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.CartaoCreditoRepository;
import com.guilherme.controlefinanceiro.repository.PasswordResetTokenRepository;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.service.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de regressão dos direitos do titular (LGPD art. 18) e da prova de
 * consentimento (art. 8, §1º).
 *
 * Cada teste falha no cenário que existia antes da implementação:
 * <ul>
 *   <li>L1 — cadastro sem aceite registrado (consentimento não comprovável);</li>
 *   <li>L2 — ausência de acesso/portabilidade (art. 18, II e V);</li>
 *   <li>L3 — ausência de eliminação de conta e histórico (art. 18, VI);</li>
 *   <li>L4 — ausência de revogação do vínculo de WhatsApp (art. 18, IX);</li>
 *   <li>R1 — credenciais vencidas acumulando no banco (arts. 15 e 16).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class LgpdApiTests {

    private static final String SENHA = "senha-segura-123";
    private static final String VERSAO_DOCUMENTOS = "teste-2026-09-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private TransacaoRepository transacoes;

    @Autowired
    private CartaoCreditoRepository cartoes;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenCleanupJob tokenCleanupJob;

    @BeforeEach
    void limparDados() {
        transacoes.deleteAll();
        cartoes.deleteAll();
        refreshTokens.deleteAll();
        resetTokens.deleteAll();
        usuarios.deleteAll();
    }

    private Usuario criarUsuario(String nome, String email) {
        return usuarios.save(new Usuario(nome, email, encoder.encode(SENHA)));
    }

    private String tokenDe(Usuario usuario) {
        return "Bearer " + jwtService.gerar(usuario);
    }

    private Transacao criarTransacao(Usuario dono, String descricao) {
        Transacao transacao = new Transacao();
        transacao.setUsuario(dono);
        transacao.setDescricao(descricao);
        transacao.setTipo("SAIDA");
        transacao.setValor(99.9);
        transacao.setData(LocalDate.now());
        transacao.setCategoria(Categoria.OUTROS);
        return transacoes.save(transacao);
    }

    /** L1 — sem aceite não há cadastro (e nada é gravado). */
    @Test
    void cadastroSemAceiteEhRecusado() throws Exception {
        mockMvc.perform(post("/auth/register")
                .header("X-Forwarded-For", "198.51.100.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Termos de Uso")));

        assertTrue(usuarios.findByEmail("ana@example.com").isEmpty(),
                "A conta foi criada mesmo sem o aceite dos documentos legais");
    }

    /** L1 — o aceite fica registrado com a versão do documento e o instante do servidor. */
    @Test
    void cadastroRegistraVersaoEInstanteDoAceite() throws Exception {
        mockMvc.perform(post("/auth/register")
                .header("X-Forwarded-For", "198.51.100.11")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"senha\":\"" + SENHA
                        + "\",\"aceiteVersao\":\"" + VERSAO_DOCUMENTOS + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aceiteVersao").value(VERSAO_DOCUMENTOS))
                .andExpect(jsonPath("$.aceiteEm").isNotEmpty());

        Usuario salvo = usuarios.findByEmail("ana@example.com").orElseThrow();
        assertEquals(VERSAO_DOCUMENTOS, salvo.getAceiteVersao(), "A versão aceita não foi gravada");
        assertNotNull(salvo.getAceiteEm(), "O instante do aceite não foi gravado");
    }

    /** L2 — a exportação traz o conjunto dos dados do titular, e só os dele. */
    @Test
    void exportacaoDeDadosTrazApenasOsDadosDoTitular() throws Exception {
        Usuario ana = criarUsuario("Ana", "ana@example.com");
        Usuario bruno = criarUsuario("Bruno", "bruno@example.com");
        criarTransacao(ana, "Mercado da Ana");
        criarTransacao(bruno, "Farmacia do Bruno");

        String corpo = mockMvc.perform(get("/usuario/dados").header(HttpHeaders.AUTHORIZATION, tokenDe(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titular.email").value("ana@example.com"))
                .andExpect(jsonPath("$.transacoes.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        assertTrue(corpo.contains("Mercado da Ana"), "O lançamento do titular não veio na exportação");
        assertFalse(corpo.contains("bruno@example.com"), "A exportação vazou dado de outro titular");
        assertFalse(corpo.contains("Farmacia do Bruno"), "A exportação vazou lançamento de outro titular");
    }

    /** L3 — exclusão de conta: exige a senha e apaga cadastro, histórico e credenciais. */
    @Test
    void exclusaoDeContaExigeSenhaEApagaTodoOHistorico() throws Exception {
        Usuario ana = criarUsuario("Ana", "ana@example.com");
        Usuario bruno = criarUsuario("Bruno", "bruno@example.com");
        criarTransacao(ana, "Mercado da Ana");
        criarTransacao(bruno, "Farmacia do Bruno");

        CartaoCredito cartao = new CartaoCredito();
        cartao.setUsuario(ana);
        cartao.setNome("Cartao da Ana");
        cartao.setLimite(1000.0);
        cartoes.save(cartao);

        String token = tokenDe(ana);

        // Sem senha e com senha errada a conta permanece: a exclusão é irreversível.
        mockMvc.perform(delete("/usuario").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/usuario").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"senha-errada\"}"))
                .andExpect(status().isBadRequest());

        assertFalse(usuarios.findByEmail("ana@example.com").isEmpty(),
                "A conta foi apagada mesmo com a senha errada");

        mockMvc.perform(delete("/usuario").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk());

        assertTrue(usuarios.findByEmail("ana@example.com").isEmpty(), "O cadastro permaneceu no banco");
        assertEquals(0, transacoes.findAllByUsuario(ana).size(), "O histórico financeiro permaneceu no banco");
        assertEquals(0, cartoes.findAllByUsuario(ana).size(), "O cartão permaneceu no banco");
        assertEquals(1, transacoes.findAllByUsuario(bruno).size(),
                "A exclusão atingiu os dados de outro titular");

        // O access token que já existia deixa de valer junto com a conta.
        mockMvc.perform(get("/usuario/dados").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    /** L4 — o vínculo de WhatsApp pode ser revogado sem excluir a conta. */
    @Test
    void vinculoDeWhatsappPodeSerRevogado() throws Exception {
        Usuario ana = criarUsuario("Ana", "ana@example.com");
        String token = tokenDe(ana);

        mockMvc.perform(post("/usuario/whatsapp").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefone\":\"5511999998888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(true));

        assertEquals("5511999998888", usuarios.findByEmail("ana@example.com").orElseThrow().getTelefone());

        mockMvc.perform(delete("/usuario/whatsapp").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculado").value(false));

        assertNull(usuarios.findByEmail("ana@example.com").orElseThrow().getTelefone(),
                "O número de WhatsApp permaneceu na conta depois da revogação");
    }

    /** R1 — o expurgo remove só credenciais vencidas (LGPD arts. 15 e 16). */
    @Test
    void expurgoRemoveApenasCredenciaisVencidas() {
        Usuario ana = criarUsuario("Ana", "ana@example.com");

        RefreshToken vencido = new RefreshToken();
        vencido.setUsuario(ana);
        vencido.setToken("refresh-vencido");
        vencido.setExpiracao(Instant.now().minusSeconds(3600));
        refreshTokens.save(vencido);

        RefreshToken valido = new RefreshToken();
        valido.setUsuario(ana);
        valido.setToken("refresh-valido");
        valido.setExpiracao(Instant.now().plusSeconds(3600));
        refreshTokens.save(valido);

        resetTokens.save(new PasswordResetToken("reset-vencido", "ana@example.com", Instant.now().minusSeconds(60)));

        tokenCleanupJob.expurgarTokensVencidos();

        List<String> restantes = refreshTokens.findAll().stream().map(RefreshToken::getToken).toList();
        assertEquals(List.of("refresh-valido"), restantes,
                "O expurgo removeu a credencial válida ou manteve a vencida");
        assertEquals(0, resetTokens.count(), "O token de recuperação vencido permaneceu no banco");
    }
}
