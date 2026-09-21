package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.RefreshToken;
import com.guilherme.controlefinanceiro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUsuario(Usuario usuario);

    /**
     * Expurgo de retencao (LGPD arts. 15 e 16): refresh token vencido e dado de
     * autenticacao sem finalidade — nao deve seguir no banco.
     */
    long countByExpiracaoBefore(Instant momento);

    void deleteByExpiracaoBefore(Instant momento);
}