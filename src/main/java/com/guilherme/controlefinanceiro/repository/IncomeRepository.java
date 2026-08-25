package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByUsuario(Usuario usuario);

    Optional<Income> findByIdAndUsuario(Long id, Usuario usuario);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.valor), 0) from Income i where i.usuario = :usuario")
    Double somarRendas(@org.springframework.data.repository.query.Param("usuario") Usuario usuario);
}
