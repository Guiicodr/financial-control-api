package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import com.guilherme.controlefinanceiro.model.Transacao;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.YearMonth;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.service.IncomeService;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;
    private final UsuarioAtualService usuarioAtual;
    private final IncomeService incomes;

    public TransacaoService(TransacaoRepository repository, UsuarioAtualService usuarioAtual, IncomeService incomes) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
        this.incomes = incomes;
    }

    public Transacao salvar(Transacao transacao) {
        transacao.setUsuario(usuarioAtual.obter());
        return repository.save(transacao);
    }

    public List<Transacao> listar() {
        return repository.findAllByUsuario(usuarioAtual.obter());
    }

    public Double calcularSaldo() {

        double rendas = incomes.totalAcumuladoAte(YearMonth.now());
        double despesas = repository.findAllByUsuario(usuarioAtual.obter()).stream()
                .filter(t -> "SAIDA".equals(t.getTipo()) && t.getValor() != null)
                .mapToDouble(Transacao::getValor).sum();
        return rendas - despesas;
    }

    public void deletar(Long id) {
        repository.findById(id).filter(item -> item.getUsuario().getId().equals(usuarioAtual.obter().getId()))
                .ifPresent(repository::delete);
    }

    public List<Map<String, Object>> gastosMensais() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        List<Transacao> transacoes = repository.findAllByUsuario(usuarioAtual.obter());
        YearMonth atual = YearMonth.now();
        for (YearMonth mes = atual.withMonth(1); !mes.isAfter(atual); mes = mes.plusMonths(1)) {
            Map<String, Object> linha = new java.util.LinkedHashMap<>();
            linha.put("month", mes.toString());
            linha.put("food", total(transacoes, mes, "ALIMENTACAO"));
            linha.put("transport", total(transacoes, mes, "TRANSPORTE"));
            linha.put("leisure", total(transacoes, mes, "LAZER"));
            linha.put("education", total(transacoes, mes, "ESTUDOS"));
            linha.put("others", total(transacoes, mes, "OUTROS"));
            resultado.add(linha);
        }
        return resultado;
    }

    private double total(List<Transacao> transacoes, YearMonth mes, String categoria) {
        return transacoes.stream()
                .filter(item -> "SAIDA".equals(item.getTipo()) && item.getData() != null && item.getCategoria() != null
                        && YearMonth.from(item.getData()).equals(mes) && categoria.equals(item.getCategoria().name()))
                .mapToDouble(Transacao::getValor).sum();
    }
}
