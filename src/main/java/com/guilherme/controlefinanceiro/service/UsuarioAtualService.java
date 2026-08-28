package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAtualService {
    private final UsuarioRepository repository;

    public UsuarioAtualService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario obter(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalStateException("Usuário não autenticado");
        return repository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));
    }

    public Usuario obter() {
        return obter(SecurityContextHolder.getContext().getAuthentication());
    }

    public Usuario salvar(Usuario usuario) {
        return repository.save(usuario);
    }
}
