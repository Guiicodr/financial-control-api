package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.dto.ProjecaoMensalDTO;
import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjecaoService {
    private final TransacaoRepository transacoes;
    private final IncomeRepository rendas;
    private final UsuarioAtualService usuarioAtual;

    public ProjecaoService(TransacaoRepository transacoes, IncomeRepository rendas, UsuarioAtualService usuarioAtual) {
        this.transacoes = transacoes;
        this.rendas = rendas;
        this.usuarioAtual = usuarioAtual;
    }

    public List<ProjecaoMensalDTO> projetar(int meses) {
        if (meses < 1 || meses > 60)
            throw new IllegalArgumentException("O período deve estar entre 1 e 60 meses");
        var usuario = usuarioAtual.obter();
        List<Transacao> despesas = transacoes.findAllByUsuario(usuario);
        List<Income> receitas = rendas.findAllByUsuario(usuario);
        List<ProjecaoMensalDTO> resultado = new ArrayList<>();
        double saldo = 0;
        for (int indice = 0; indice < meses; indice++) {
            YearMonth mes = YearMonth.now().plusMonths(indice);
            double entrada = receitas.stream().filter(item -> item.getValor() != null).mapToDouble(Income::getValor)
                    .sum();
            double saida = despesas.stream()
                    .filter(item -> item.getTipo() != null && item.getTipo().equals("SAIDA") && ativoNoMes(item, mes))
                    .mapToDouble(this::valorMensal).sum();
            saldo += entrada - saida;
            resultado.add(new ProjecaoMensalDTO(mes, entrada, saida, saldo));
        }
        return resultado;
    }

    private boolean ativoNoMes(Transacao item, YearMonth mes) {
        if (item.getData() == null)
            return false;
        YearMonth inicio = YearMonth.from(item.getData());
        return !mes.isBefore(inicio)
                && (item.getTotalParcelas() == null || item.getTotalParcelas() <= 1 || mes.getMonthValue()
                        - inicio.getMonthValue() + 12 * (mes.getYear() - inicio.getYear()) < item.getTotalParcelas());
    }

    private double valorMensal(Transacao item) {
        return item.getTotalParcelas() != null && item.getTotalParcelas() > 1
                ? item.getValor() / item.getTotalParcelas()
                : item.getValor();
    }
}
