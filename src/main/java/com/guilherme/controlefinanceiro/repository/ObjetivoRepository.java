package com.guilherme.controlefinanceiro.repository;

import com.guilherme.controlefinanceiro.model.Objetivo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetivoRepository extends JpaRepository<Objetivo, Long> {
}