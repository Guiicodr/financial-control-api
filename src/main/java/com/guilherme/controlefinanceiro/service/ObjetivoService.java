package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Objetivo;
import com.guilherme.controlefinanceiro.repository.ObjetivoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import com.guilherme.controlefinanceiro.model.Usuario;

@Service
public class ObjetivoService {

    private final ObjetivoRepository repository;
    private final UsuarioAtualService usuarioAtual;

    public ObjetivoService(ObjetivoRepository repository, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    public Objetivo salvar(Objetivo objetivo) {
        objetivo.setUsuario(usuarioAtual.obter());
        return repository.save(objetivo);
    }

    public List<Objetivo> listar() {
        return repository.findAllByUsuario(usuarioAtual.obter());
    }

    public void deletar(Long id) {
        repository.findById(id).filter(item -> item.getUsuario().getId().equals(usuarioAtual.obter().getId()))
                .ifPresent(repository::delete);
    }
}