package com.guilherme.controlefinanceiro.util;

/**
 * Mascaramento de dados pessoais para log.
 *
 * A LGPD exige proporcionalidade entre o que se registra e a finalidade do
 * registro (arts. 6, III e 46). Log de aplicação não precisa do e-mail completo
 * nem do telefone integral para provar quem fez o quê.
 *
 * Centralizar aqui evita o cenário que existia antes: o AuthService já mascarava
 * o e-mail, enquanto o WhatsAppService gravava o endereço completo junto do
 * valor do lançamento — dois critérios para a mesma categoria de dado.
 */
public final class PiiMasker {

    private PiiMasker() {
    }

    /** Ex.: gu***@gmail.com — suficiente para auditoria sem expor o endereço todo. */
    public static String email(String email) {
        if (email == null || email.isBlank())
            return "***";
        int arroba = email.indexOf('@');
        if (arroba < 0)
            return "***";
        if (arroba <= 2)
            return "***" + email.substring(arroba);
        return email.substring(0, 2) + "***" + email.substring(arroba);
    }

    /** Ex.: 5511****8888 — mantém DDI/DDD e os quatro últimos dígitos. */
    public static String telefone(String telefone) {
        if (telefone == null || telefone.isBlank())
            return "***";
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() <= 4)
            return "***";
        int prefixo = digitos.length() > 8 ? 4 : 2;
        return digitos.substring(0, prefixo) + "****" + digitos.substring(digitos.length() - 4);
    }
}
