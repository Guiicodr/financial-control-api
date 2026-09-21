package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;
import com.guilherme.controlefinanceiro.model.Usuario;
import java.util.List;

public interface ObjetivoRepository extends JpaRepository<Objetivo, Long> {
    List<Objetivo> findAllByUsuario(Usuario usuario);

    /** Exclusao de conta: apaga as metas do titular (LGPD art. 18, VI). */
    void deleteAllByUsuario(Usuario usuario);
}