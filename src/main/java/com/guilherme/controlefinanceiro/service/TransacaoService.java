package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import com.guilherme.controlefinanceiro.model.Transacao;

import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public Transacao salvar(Transacao transacao){
        return repository.save(transacao);
    }

    public List<Transacao> listar() {
        return repository.findAll();
    }

    public Double calcularSaldo(){

        List<Transacao> transacoes = repository.findAll();
        double saldo = 0;

        for (Transacao t : transacoes){
            if (t.getTipo().equals("ENTRADA")){
                saldo += t.getValor();
            } else if (t.getTipo().equals("SAIDA")){
                saldo -= t.getValor();
            }
        }
        return saldo;
    }
}
