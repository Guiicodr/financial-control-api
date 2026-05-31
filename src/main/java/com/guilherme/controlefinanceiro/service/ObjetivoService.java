package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Objetivo;
import com.guilherme.controlefinanceiro.repository.ObjetivoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObjetivoService {

    private final ObjetivoRepository repository;

    public ObjetivoService(ObjetivoRepository repository) {
        this.repository = repository;
    }

    public Objetivo salvar(Objetivo objetivo) {
        return repository.save(objetivo);
    }

    public List<Objetivo> listar() {
        return repository.findAll();
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }
}