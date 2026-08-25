package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.MovimentoMeta;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimentoMetaRepository extends JpaRepository<MovimentoMeta, Long> {
    List<MovimentoMeta> findAllByUsuario(Usuario usuario);
}