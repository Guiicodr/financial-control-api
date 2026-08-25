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
}