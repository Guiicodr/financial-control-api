package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.guilherme.controlefinanceiro.model.TipoRenda;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findAllByUsuario(Usuario usuario);

    Optional<Income> findByIdAndUsuario(Long id, Usuario usuario);

    Optional<Income> findByUsuarioAndTipo(Usuario usuario, TipoRenda tipo);

}
