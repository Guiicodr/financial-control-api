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
}
