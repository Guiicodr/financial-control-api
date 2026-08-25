package com.guilherme.controlefinanceiro.dto;

import com.guilherme.controlefinanceiro.model.Categoria;
import java.time.LocalDate;

public record ImportacaoTransacaoDTO(String descricao, Double valor, String tipo, LocalDate data, Categoria categoria) {
}
