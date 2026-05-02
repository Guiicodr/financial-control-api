package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
// Intermédio do banco de dados
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
}
