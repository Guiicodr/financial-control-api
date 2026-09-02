package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUtilizadoFalse(String token);
    void deleteByEmail(String email);
}