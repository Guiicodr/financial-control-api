package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUtilizadoFalse(String token);
    void deleteByEmail(String email);

    /** Expurgo de retencao (LGPD arts. 15 e 16): token de recuperacao vencido nao tem finalidade. */
    long countByExpiracaoBefore(Instant momento);

    void deleteByExpiracaoBefore(Instant momento);
}