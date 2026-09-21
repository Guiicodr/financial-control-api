package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.service.ContaService;
import com.guilherme.controlefinanceiro.service.UsuarioAtualService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Direitos do titular sobre os próprios dados (LGPD art. 18).
 *
 * São duas operações que o app expõe na página de perfil:
 * <ul>
 *   <li>{@code GET /usuario/dados} — acesso e portabilidade (art. 18, II e V),
 *       devolvendo o JSON com tudo o que a API guarda sobre quem chamou;</li>
 *   <li>{@code DELETE /usuario} — eliminação da conta e do histórico
 *       (art. 18, VI), confirmada com a senha do próprio titular.</li>
 * </ul>
 * Nenhum dos dois aceita id no caminho: o alvo é SEMPRE o usuário autenticado,
 * o que elimina qualquer possibilidade de um titular operar dados de outro.
 */
@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioAtualService usuarioAtual;
    private final ContaService conta;

    public UsuarioController(UsuarioAtualService usuarioAtual, ContaService conta) {
        this.usuarioAtual = usuarioAtual;
        this.conta = conta;
    }

    @GetMapping("/dados")
    public Map<String, Object> meusDados() {
        return conta.exportar(usuarioAtual.obter());
    }

    @DeleteMapping
    public Map<String, String> excluirConta(@RequestBody(required = false) Map<String, String> body) {
        String senha = body == null ? null : body.get("senha");
        conta.excluirConta(usuarioAtual.obter(), senha);
        return Map.of(
                "mensagem", "Conta e dados pessoais eliminados. Não guardamos cópia deste histórico.");
    }
}
