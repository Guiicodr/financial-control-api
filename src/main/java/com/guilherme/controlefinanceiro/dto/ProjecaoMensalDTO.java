package com.guilherme.controlefinanceiro.dto;

import java.time.YearMonth;

public record ProjecaoMensalDTO(YearMonth mes, Double receitas, Double despesas, Double saldoProjetado) {
}
