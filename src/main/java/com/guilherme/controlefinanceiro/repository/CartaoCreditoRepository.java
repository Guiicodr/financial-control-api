package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.CartaoCredito;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {
    List<CartaoCredito> findAllByUsuario(Usuario usuario);
}