package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.CartaoCredito;
import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.model.MovimentoMeta;
import com.guilherme.controlefinanceiro.model.Objetivo;
import com.guilherme.controlefinanceiro.model.Orcamento;
import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.CartaoCreditoRepository;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import com.guilherme.controlefinanceiro.repository.MovimentoMetaRepository;
import com.guilherme.controlefinanceiro.repository.ObjetivoRepository;
import com.guilherme.controlefinanceiro.repository.OrcamentoRepository;
import com.guilherme.controlefinanceiro.repository.PasswordResetTokenRepository;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.util.PiiMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Direitos do titular sobre os próprios dados (LGPD art. 18).
 *
 * Antes deste serviço não existia porta para os incisos II a VI do art. 18: o
 * titular conseguia excluir lançamentos avulsos, mas não acessar/portar o
 * conjunto dos seus dados nem eliminar a conta (nome, e-mail, telefone e todo o
 * histórico financeiro permaneciam no banco para sempre).
 *
 * Aqui ficam as duas operações que faltavam:
 * <ul>
 *   <li>art. 18, II e V — acesso e portabilidade: {@link #exportar(Usuario)};</li>
 *   <li>art. 18, VI — eliminação: {@link #excluirConta(Usuario, String)}.</li>
 * </ul>
 */
@Service
public class ContaService {

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);

    private final UsuarioRepository usuarios;
    private final TransacaoRepository transacoes;
    private final IncomeRepository incomes;
    private final ObjetivoRepository objetivos;
    private final MovimentoMetaRepository movimentos;
    private final OrcamentoRepository orcamentos;
    private final CartaoCreditoRepository cartoes;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final AuthenticationManager authenticationManager;
    private final String versaoDocumentos;

    public ContaService(UsuarioRepository usuarios,
            TransacaoRepository transacoes,
            IncomeRepository incomes,
            ObjetivoRepository objetivos,
            MovimentoMetaRepository movimentos,
            OrcamentoRepository orcamentos,
            CartaoCreditoRepository cartoes,
            RefreshTokenRepository refreshTokens,
            PasswordResetTokenRepository resetTokens,
            AuthenticationManager authenticationManager,
            @Value("${app.legal.terms-version:2026-09-1}") String versaoDocumentos) {
        this.usuarios = usuarios;
        this.transacoes = transacoes;
        this.incomes = incomes;
        this.objetivos = objetivos;
        this.movimentos = movimentos;
        this.orcamentos = orcamentos;
        this.cartoes = cartoes;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.authenticationManager = authenticationManager;
        this.versaoDocumentos = versaoDocumentos;
    }

    /**
     * Exporta, em JSON, tudo o que a API guarda sobre o titular autenticado.
     * Formato legível e estruturado (art. 18, II e V) — mesmo conteúdo exibido
     * no app, sem dado de terceiros e sem hash de senha.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportar(Usuario usuario) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("geradoEm", Instant.now().toString());
        dados.put("versaoDocumentos", versaoDocumentos);
        dados.put("titular", dadosDoTitular(usuario));
        dados.put("transacoes", transacoes.findAllByUsuario(usuario).stream().map(this::transacao).toList());
        dados.put("rendas", incomes.findAllByUsuario(usuario).stream().map(this::renda).toList());
        dados.put("objetivos", objetivos.findAllByUsuario(usuario).stream().map(this::objetivo).toList());
        dados.put("movimentosDeMetas", movimentos.findAllByUsuario(usuario).stream().map(this::movimento).toList());
        dados.put("orcamentos", orcamentos.findAllByUsuario(usuario).stream().map(this::orcamento).toList());
        dados.put("cartoes", cartoes.findAllByUsuario(usuario).stream().map(this::cartao).toList());
        return dados;
    }

    /**
     * Elimina a conta e todo o histórico do titular (art. 18, VI).
     *
     * Exige a senha: o access token pode estar em um navegador compartilhado e a
     * operação é irreversível. A ordem das exclusões segue as chaves estrangeiras
     * (movimento → transação → cartão → meta → orçamento → renda → tokens → usuário).
     */
    @Transactional
    public void excluirConta(Usuario usuario, String senha) {
        if (senha == null || senha.isBlank())
            throw new IllegalArgumentException("Informe sua senha para confirmar a exclusão da conta");

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(usuario.getEmail(), senha));
        } catch (Exception e) {
            log.info("Exclusão de conta recusada por senha inválida: {}", PiiMasker.email(usuario.getEmail()));
            throw new IllegalArgumentException("Senha incorreta");
        }

        movimentos.deleteAllByUsuario(usuario);
        transacoes.deleteAllByUsuario(usuario);
        cartoes.deleteAllByUsuario(usuario);
        objetivos.deleteAllByUsuario(usuario);
        orcamentos.deleteAllByUsuario(usuario);
        incomes.deleteAllByUsuario(usuario);
        refreshTokens.deleteByUsuario(usuario);
        resetTokens.deleteByEmail(usuario.getEmail());
        usuarios.delete(usuario);

        log.info("Conta e dados eliminados a pedido do titular: {}", PiiMasker.email(usuario.getEmail()));
    }

    // ---------------------------- Projecoes do JSON ----------------------------

    private Map<String, Object> dadosDoTitular(Usuario usuario) {
        Map<String, Object> titular = new LinkedHashMap<>();
        titular.put("id", usuario.getId());
        titular.put("nome", usuario.getName());
        titular.put("email", usuario.getEmail());
        titular.put("telefone", usuario.getTelefone());
        titular.put("aceiteVersao", usuario.getAceiteVersao());
        titular.put("aceiteEm", usuario.getAceiteEm() == null ? null : usuario.getAceiteEm().toString());
        return titular;
    }

    private Map<String, Object> transacao(Transacao item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("descricao", item.getDescricao());
        linha.put("valor", item.getValor());
        linha.put("tipo", item.getTipo());
        linha.put("data", item.getData() == null ? null : item.getData().toString());
        linha.put("categoria", item.getCategoria() == null ? null : item.getCategoria().name());
        return linha;
    }

    private Map<String, Object> renda(Income item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("descricao", item.getDescricao());
        linha.put("valor", item.getValor());
        linha.put("data", item.getData() == null ? null : item.getData().toString());
        linha.put("tipo", item.getTipo() == null ? null : item.getTipo().name());
        return linha;
    }

    private Map<String, Object> objetivo(Objetivo item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("nome", item.getNome());
        linha.put("valorAlvo", item.getValorAlvo());
        linha.put("valorAtual", item.getValorAtual());
        linha.put("prazo", item.getPrazo() == null ? null : item.getPrazo().toString());
        linha.put("tipo", item.getTipo() == null ? null : item.getTipo().name());
        return linha;
    }

    private Map<String, Object> movimento(MovimentoMeta item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("objetivoId", item.getObjetivo() == null ? null : item.getObjetivo().getId());
        linha.put("valor", item.getValor());
        linha.put("tipo", item.getTipo());
        linha.put("data", item.getData() == null ? null : item.getData().toString());
        return linha;
    }

    private Map<String, Object> orcamento(Orcamento item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("categoria", item.getCategoria() == null ? null : item.getCategoria().name());
        linha.put("limiteMensal", item.getLimiteMensal());
        return linha;
    }

    private Map<String, Object> cartao(CartaoCredito item) {
        Map<String, Object> linha = new LinkedHashMap<>();
        linha.put("id", item.getId());
        linha.put("nome", item.getNome());
        linha.put("limite", item.getLimite());
        linha.put("diaFechamento", item.getDiaFechamento());
        linha.put("diaVencimento", item.getDiaVencimento());
        return linha;
    }
}
