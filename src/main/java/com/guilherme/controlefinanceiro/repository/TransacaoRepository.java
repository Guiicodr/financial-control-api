package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import com.guilherme.controlefinanceiro.model.Usuario;
import java.util.List;

// Intermédio do banco de dados
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findAllByUsuario(Usuario usuario);
}
