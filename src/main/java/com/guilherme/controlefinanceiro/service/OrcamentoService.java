package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Orcamento;
import com.guilherme.controlefinanceiro.repository.OrcamentoRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
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

    public Orcamento salvar(Orcamento item) {
        item.setUsuario(usuarioAtual.obter());
        return orcamentos.save(item);
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
