package com.guilherme.controlefinanceiro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.guilherme.controlefinanceiro.util.PiiMasker;

/**
 * O log da API não deve carregar dado pessoal em claro (LGPD arts. 6º, III e 46).
 *
 * O caso que motivou o utilitário: o WhatsAppService gravava o e-mail completo do
 * titular junto do valor do lançamento, enquanto o AuthService já mascarava o
 * mesmo dado — dois critérios para a mesma categoria de informação.
 */
class PiiMaskerTests {

    @Test
    void emailMantemApenasOPrefixoEDominio() {
        assertEquals("gu***@gmail.com", PiiMasker.email("guilherme@gmail.com"));
        assertEquals("***@x.com", PiiMasker.email("ab@x.com"));
        assertEquals("***", PiiMasker.email("sem-arroba"));
        assertEquals("***", PiiMasker.email(null));
    }

    @Test
    void telefoneMantemDdiEQuatroUltimosDigitos() {
        assertEquals("5511****8888", PiiMasker.telefone("whatsapp:+5511999998888"));
        assertEquals("1199****8888", PiiMasker.telefone("(11) 99999-8888"));
        assertEquals("***", PiiMasker.telefone("123"));
        assertEquals("***", PiiMasker.telefone(""));
    }
}
