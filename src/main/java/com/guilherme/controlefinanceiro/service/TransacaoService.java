package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import com.guilherme.controlefinanceiro.model.Transacao;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.YearMonth;
import com.guilherme.controlefinanceiro.model.Usuario;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;
    private final UsuarioAtualService usuarioAtual;

    public TransacaoService(TransacaoRepository repository, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    public Transacao salvar(Transacao transacao) {
        transacao.setUsuario(usuarioAtual.obter());
        return repository.save(transacao);
    }

    public List<Transacao> listar() {
        return repository.findAllByUsuario(usuarioAtual.obter());
    }

    public Double calcularSaldo() {

        List<Transacao> transacoes = repository.findAllByUsuario(usuarioAtual.obter());
        double saldo = 0;

        for (Transacao t : transacoes) {
            if (t.getTipo().equals("ENTRADA")) {
                saldo += t.getValor();
            } else if (t.getTipo().equals("SAIDA")) {
                saldo -= t.getValor();
            }
        }
        return saldo;
    }

    public void deletar(Long id) {
        repository.findById(id).filter(item -> item.getUsuario().getId().equals(usuarioAtual.obter().getId()))
                .ifPresent(repository::delete);
    }

    public List<Map<String, Object>> gastosMensais() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        List<Transacao> transacoes = repository.findAllByUsuario(usuarioAtual.obter());
        for (int indice = 11; indice >= 0; indice--) {
            YearMonth mes = YearMonth.now().minusMonths(indice);
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
