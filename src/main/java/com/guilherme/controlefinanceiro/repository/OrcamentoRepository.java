package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Orcamento;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    List<Orcamento> findAllByUsuario(Usuario usuario);

    Optional<Orcamento> findByCategoriaAndUsuario(com.guilherme.controlefinanceiro.model.Categoria categoria,
            Usuario usuario);

    /** Exclusao de conta: apaga os limites de orcamento do titular (LGPD art. 18, VI). */
    void deleteAllByUsuario(Usuario usuario);
}