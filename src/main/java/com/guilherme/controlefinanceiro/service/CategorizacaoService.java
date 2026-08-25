package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Categoria;
import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@Service
public class CategorizacaoService {
    private static final Map<Categoria, String[]> TERMOS = Map.of(
            Categoria.ALIMENTACAO,
            new String[] { "ifood", "uber eats", "restaurante", "mercado", "supermercado", "padaria" },
            Categoria.TRANSPORTE, new String[] { "uber", "99", "posto", "combustivel", "estacionamento", "metro" },
            Categoria.LAZER, new String[] { "netflix", "spotify", "cinema", "jogo", "steam" },
            Categoria.ESTUDOS, new String[] { "curso", "faculdade", "livro", "udemy", "escola" });

    public Categoria categorizar(String descricao) {
        String texto = Normalizer.normalize(descricao == null ? "" : descricao, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return TERMOS.entrySet().stream()
                .filter(entry -> java.util.Arrays.stream(entry.getValue()).anyMatch(texto::contains))
                .map(Map.Entry::getKey).findFirst().orElse(Categoria.OUTROS);
    }
}
