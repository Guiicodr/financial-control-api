package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.CartaoCredito;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {
    List<CartaoCredito> findAllByUsuario(Usuario usuario);

    /** Exclusao de conta: apaga os cartoes do titular (LGPD art. 18, VI). */
    void deleteAllByUsuario(Usuario usuario);
}