package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Categoria;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Interpreta mensagens de texto livre enviadas pelo WhatsApp e extrai
 * valor, tipo (ENTRADA/SAIDA), categoria e descrição.
 *
 * Exemplos aceitos:
 *   "gastei 25,50 no almoço"        -> SAIDA  25.50  ALIMENTACAO  "no almoço"
 *   "paguei 120 de uber"            -> SAIDA  120.00 TRANSPORTE  "de uber"
 *   "recebi 3000 de salario"        -> ENTRADA 3000.00 SALARIO     "de salario"
 *   "lanche 15,90"                  -> SAIDA  15.90  ALIMENTACAO  "lanche"
 *   "cinema 45 lazer"               -> SAIDA  45.00  LAZER        "cinema"
 *   "uber 30 transporte"            -> SAIDA  30.00  TRANSPORTE   "uber"
 *   "curso 200 estudos"             -> SAIDA  200.00 ESTUDOS      "curso"
 */
public final class WhatsAppMessageParser {

    public record Parsed(
            BigDecimal valor,
            String tipo,          // "ENTRADA" ou "SAIDA"
            Categoria categoria,
            String descricao) {
    }

    private WhatsAppMessageParser() {
    }

    public static Parsed parse(String mensagem) {
        if (mensagem == null || mensagem.isBlank())
            return null;

        String texto = mensagem.trim();

        BigDecimal valor = extrairValor(texto);
        if (valor == null || valor.signum() <= 0)
            return null;

        String tipo = detectarTipo(texto);
        Categoria categoria = detectarCategoria(texto, tipo);
        String descricao = extrairDescricao(texto);

        return new Parsed(valor, tipo, categoria, descricao);
    }

    /**
     * Extrai o primeiro valor monetário da mensagem, aceitando:
     * "25,50", "25.50", "R$ 1.234,56", "R$25", "1200", "1,200.00"
     */
    static BigDecimal extrairValor(String texto) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:r\\$\\s*)?(\\d{1,3}(?:[.,]\\d{3})+|[\\d]+)(?:[.,](\\d{1,2}))?")
                .matcher(texto.toLowerCase(Locale.ROOT));

        if (!m.find())
            return null;

        String inteiro = m.group(1);
        String centavos = m.group(2);

        // Remove separadores de milhar e normaliza para ponto decimal
        inteiro = inteiro.replaceAll("[.,]", "");
        if (centavos == null)
            centavos = "00";

        try {
            return new BigDecimal(inteiro + "." + centavos);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String detectarTipo(String texto) {
        String t = texto.toLowerCase(Locale.ROOT);
        if (t.contains("recebi") || t.contains("salario") || t.contains("salário")
                || t.contains("ganhei") || t.contains("entrada") || t.contains("renda")
                || t.contains("freela") || t.contains("reembolso"))
            return "ENTRADA";
        return "SAIDA";
    }

    static Categoria detectarCategoria(String texto, String tipo) {
        String t = normalizar(texto);

        if (tipo.equals("ENTRADA"))
            return Categoria.SALARIO;

        if (contem(t, "almoco", "almoço", "jantar", "cafe", "café", "comida", "mercado",
                "restaurante", "lanche", "pizza", "ifood", "supermercado", "padaria", "snack"))
            return Categoria.ALIMENTACAO;

        if (contem(t, "uber", "99", "onibus", "ônibus", "metro", "metrô", "combustivel",
                "combustível", "gasolina", "estacionamento", "pedagio", "pedágio", "trem", "taxi", "táxi"))
            return Categoria.TRANSPORTE;

        if (contem(t, "cinema", "bar", "show", "festa", "jogo", "streaming", "netflix",
                "spotify", "viagem", "passeio", "lazer"))
            return Categoria.LAZER;

        if (contem(t, "curso", "faculdade", "livro", "escola", "aula", "mensalidade",
                "estudo", "estudos", "certificacao", "certificação"))
            return Categoria.ESTUDOS;

        if (contem(t, "alimentacao", "alimentação", "transporte", "outros"))
            return Categoria.valueOf(t.contains("alimentacao") || t.contains("alimentação")
                    ? "ALIMENTACAO"
                    : t.contains("transporte") ? "TRANSPORTE" : "OUTROS");

        return Categoria.OUTROS;
    }

    /**
     * Remove do texto o valor e palavras-chave de comando, deixando a
     * descrição restante (limitada a 120 caracteres).
     */
    static String extrairDescricao(String texto) {
        String semValor = texto
                .replaceAll("(?:r\\$\\s*)?\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?", " ")
                .replaceAll("(?:r\\$\\s*)?\\d+(?:[.,]\\d{1,2})?", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        if (semValor.isBlank())
            return "Lançamento via WhatsApp";

        return semValor.length() > 120 ? semValor.substring(0, 120) : semValor;
    }

    private static String normalizar(String texto) {
        return texto.toLowerCase(Locale.ROOT)
                .replace("ç", "c")
                .replace("ã", "a").replace("á", "a").replace("â", "a").replace("à", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u");
    }

    private static boolean contem(String textoNormalizado, String... palavras) {
        for (String palavra : palavras) {
            if (textoNormalizado.contains(normalizar(palavra)))
                return true;
        }
        return false;
    }
}