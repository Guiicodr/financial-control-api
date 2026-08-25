package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.repository.CartaoCreditoRepository;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import com.guilherme.controlefinanceiro.service.OrcamentoService;
import com.guilherme.controlefinanceiro.service.UsuarioAtualService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {
    private final OrcamentoService orcamentos;
    private final IncomeRepository rendas;
    private final CartaoCreditoRepository cartoes;
    private final UsuarioAtualService usuarioAtual;

    public NotificacaoController(OrcamentoService orcamentos, IncomeRepository rendas, CartaoCreditoRepository cartoes,
            UsuarioAtualService usuarioAtual) {
        this.orcamentos = orcamentos;
        this.rendas = rendas;
        this.cartoes = cartoes;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        var usuario = usuarioAtual.obter();
        var resultado = new ArrayList<>(orcamentos.alertas().stream()
                .filter(item -> !"NORMAL".equals(item.get("alerta"))).map(item -> Map.<String, Object>of("tipo",
                        "ORCAMENTO", "titulo", "Limite de " + item.get("categoria"), "mensagem", item.get("alerta")))
                .toList());
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(3);
        for (Income renda : rendas.findAllByUsuario(usuario))
            if (renda.getData() != null && !renda.getData().isBefore(hoje) && !renda.getData().isAfter(limite))
                resultado.add(Map.of("tipo", "VENCIMENTO", "titulo", "Vencimento próximo", "mensagem",
                        renda.getDescricao() + " vence em " + renda.getData()));
        for (var cartao : cartoes.findAllByUsuario(usuario)) {
            int dia = cartao.getDiaVencimento() == null ? 0 : cartao.getDiaVencimento();
            LocalDate vencimento = hoje.withDayOfMonth(Math.min(dia, hoje.lengthOfMonth()));
            if (vencimento.isBefore(hoje))
                vencimento = vencimento.plusMonths(1);
            if (!vencimento.isAfter(limite))
                resultado.add(Map.of("tipo", "FATURA", "titulo", "Fatura próxima", "mensagem",
                        cartao.getNome() + " vence em " + vencimento));
        }
        return resultado;
    }
}
