package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Orcamento;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.OrcamentoRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class OrcamentoService {
    private final OrcamentoRepository orcamentos;
    private final TransacaoRepository transacoes;
    private final UsuarioAtualService usuarioAtual;

    public OrcamentoService(OrcamentoRepository orcamentos, TransacaoRepository transacoes,
            UsuarioAtualService usuarioAtual) {
        this.orcamentos = orcamentos;
        this.transacoes = transacoes;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * POST /orcamentos é um upsert por (usuário, categoria).
     *
     * O front (BudgetManager) envia apenas {categoria, limiteMensal}, sem id, tanto
     * ao definir quanto ao editar um limite. Sem o merge abaixo, a segunda gravação
     * violava a constraint única (usuario_id, categoria) e o usuário recebia 500 ao
     * editar um limite já existente.
     */
    @Transactional
    public Orcamento salvar(Orcamento item) {
        if (item.getCategoria() == null)
            throw new IllegalArgumentException("Categoria é obrigatória");
        if (item.getLimiteMensal() == null || item.getLimiteMensal() <= 0)
            throw new IllegalArgumentException("Informe um limite maior que zero.");

        Usuario usuario = usuarioAtual.obter();
        // O dono vem SEMPRE do token; o id do corpo é ignorado pelo Jackson
        // (@JsonProperty READ_ONLY no modelo), então não há como sobrescrever
        // o orçamento de outro usuário.
        Orcamento destino = orcamentos.findByCategoriaAndUsuario(item.getCategoria(), usuario)
                .orElseGet(Orcamento::new);

        destino.setCategoria(item.getCategoria());
        destino.setLimiteMensal(item.getLimiteMensal());
        destino.setUsuario(usuario);
        return orcamentos.save(destino);
    }

    public List<Orcamento> listar() {
        return orcamentos.findAllByUsuario(usuarioAtual.obter());
    }
    public List<Map<String, Object>> alertas() {
        var usuario = usuarioAtual.obter();
        var mes = YearMonth.now();
        return orcamentos.findAllByUsuario(usuario).stream().map(item -> {
            double gasto = transacoes.findAllByUsuario(usuario).stream()
                    .filter(t -> t.getCategoria() == item.getCategoria() && "SAIDA".equals(t.getTipo())
                            && t.getData() != null && YearMonth.from(t.getData()).equals(mes))
                    .mapToDouble(t -> t.getValor() == null ? 0 : t.getValor()).sum();
            double percentual = item.getLimiteMensal() == 0 ? 0 : gasto / item.getLimiteMensal() * 100;
            return Map.<String, Object>of("categoria", item.getCategoria(), "limite", item.getLimiteMensal(), "gasto",
                    gasto, "percentual", percentual, "alerta",
                    percentual >= 100 ? "LIMITE_ATINGIDO" : percentual >= 80 ? "ATENCAO" : "NORMAL");
        }).toList();
    }
}
