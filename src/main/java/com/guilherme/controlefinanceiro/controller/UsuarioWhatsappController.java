package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.service.UsuarioAtualService;
import com.guilherme.controlefinanceiro.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Permite ao usuário autenticado vincular/consultar o número de WhatsApp
 * da sua conta. O número é normalizado (apenas dígitos) antes de salvar.
 */
@RestController
@RequestMapping("/usuario/whatsapp")
public class UsuarioWhatsappController {

    private final UsuarioAtualService usuarioAtual;
    private final WhatsAppService whatsAppService;
    private final String botNumero;

    public UsuarioWhatsappController(UsuarioAtualService usuarioAtual, WhatsAppService whatsAppService,
            @Value("${app.whatsapp.bot-number:}") String botNumero) {
        this.usuarioAtual = usuarioAtual;
        this.whatsAppService = whatsAppService;
        this.botNumero = botNumero == null ? "" : botNumero;
    }

    @GetMapping
    public Map<String, Object> consultar() {
        Usuario usuario = usuarioAtual.obter();
        return Map.of(
                "telefone", usuario.getTelefone() == null ? "" : usuario.getTelefone(),
                "vinculado", usuario.getTelefone() != null && !usuario.getTelefone().isBlank(),
                "botNumero", botNumero);
    }

    @PostMapping
    public Map<String, Object> vincular(@RequestBody Map<String, String> body) {
        String bruto = body.getOrDefault("telefone", body.get("phone"));
        String normalizado = WhatsAppService.normalizarTelefone(bruto);

        if (normalizado == null || normalizado.length() < 10 || normalizado.length() > 15) {
            throw new IllegalArgumentException("Informe um número de WhatsApp válido com DDI (ex.: 5511999998888)");
        }

        Usuario usuario = usuarioAtual.obter();
        usuario.setTelefone(normalizado);
        usuarioAtual.salvar(usuario);

        return Map.of(
                "telefone", normalizado,
                "vinculado", true,
                "botNumero", botNumero,
                "mensagem", "Número vinculado! Envie 'ajuda' para o bot no WhatsApp para ver os comandos.");
    }
}