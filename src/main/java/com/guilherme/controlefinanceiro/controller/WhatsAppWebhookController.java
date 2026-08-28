package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook que recebe mensagens do WhatsApp e responde com a confirmação
 * do lançamento.
 *
 * Suporta dois provedores (escolha um na plataforma):
 *
 * 1) Twilio WhatsApp (form-urlencoded):
 *    - POST /webhooks/whatsapp  (Messaging → "When a message comes in")
 *    - Campos: From=whatsapp:+5511..., Body=gastei 25,50 no almoço
 *
 * 2) Meta WhatsApp Cloud API (JSON):
 *    - GET  /webhooks/whatsapp  (verificação com hub.challenge)
 *    - POST /webhooks/whatsapp  (mensagens; entry[0].changes[0].value.messages[0])
 *
 * Segurança opcional: defina WHATSAPP_WEBHOOK_TOKEN no Railway e configure o
 * mesmo valor no provedor (query ?token=... ou header X-Webhook-Token).
 */
@RestController
public class WhatsAppWebhookController {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService service;
    private final String webhookToken;

    public WhatsAppWebhookController(WhatsAppService service,
            @Value("${app.whatsapp.webhook-token:}") String webhookToken) {
        this.service = service;
        this.webhookToken = webhookToken == null ? "" : webhookToken;
    }

    /** Verificação do webhook da Meta (WhatsApp Cloud API). */
    @GetMapping(value = "/webhooks/whatsapp", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificarMeta(
            @RequestParam(value = "hub.mode", required = false) String hubMode,
            @RequestParam(value = "hub.verify_token", required = false) String hubVerifyToken,
            @RequestParam(value = "hub.challenge", required = false) String hubChallenge) {

        if (hubChallenge == null)
            return ResponseEntity.badRequest().body("hub.challenge ausente");

        // Se um token de verificação estiver configurado, valide; caso contrário,
        // apenas ecoe o challenge (modo de teste).
        String verifyToken = System.getenv().getOrDefault("WHATSAPP_VERIFY_TOKEN", "");
        if (!verifyToken.isBlank() && !verifyToken.equals(hubVerifyToken)) {
            return ResponseEntity.status(403).body("verify_token inválido");
        }
        return ResponseEntity.ok(hubChallenge);
    }

    /** Recebe mensagens do Twilio (form) ou da Meta (JSON). */
    @PostMapping(value = "/webhooks/whatsapp", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<String> receber(
            @RequestParam(required = false) Map<String, String> form,
            @RequestBody(required = false) Map<String, Object> json,
            @RequestHeader(value = "X-Webhook-Token", required = false) String headerToken) {

        if (!webhookToken.isBlank() && !webhookToken.equals(headerToken)
                && !webhookToken.equals(form.get("token"))) {
            return ResponseEntity.status(401).build();
        }

        try {
            if (json != null && !json.isEmpty()) {
                return responder(extrairMeta(json));
            }
            if (form != null && !form.isEmpty()) {
                String from = form.getOrDefault("From", form.getOrDefault("WaId", ""));
                String body = form.getOrDefault("Body", form.getOrDefault("Texto", ""));
                return responder(from, body);
            }
        } catch (Exception e) {
            // Nunca derrube o webhook: provedores desativam endpoints que falham.
            log.error("Falha ao processar mensagem do WhatsApp", e);
        }

        return ResponseEntity.ok("ok");
    }

    private ResponseEntity<String> responder(String telefone, String mensagem) {
        String resposta = service.processarMensagem(telefone, mensagem);
        return ResponseEntity.ok(resposta);
    }

    private ResponseEntity<String> responder(Object[] dados) {
        if (dados == null || dados.length < 2)
            return ResponseEntity.ok("ok");
        return responder((String) dados[0], (String) dados[1]);
    }

    /** Extrai [telefone, mensagem] do payload da Meta WhatsApp Cloud API. */
    @SuppressWarnings("unchecked")
    private Object[] extrairMeta(Map<String, Object> json) {
        try {
            var entry = (java.util.List<Map<String, Object>>) json.get("entry");
            if (entry == null || entry.isEmpty())
                return null;
            var changes = (java.util.List<Map<String, Object>>) entry.get(0).get("changes");
            if (changes == null || changes.isEmpty())
                return null;
            var value = (Map<String, Object>) changes.get(0).get("value");
            if (value == null)
                return null;
            var messages = (java.util.List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty())
                return null; // status updates etc.
            var msg = messages.get(0);
            String from = String.valueOf(msg.get("from"));
            String texto;
            if (msg.containsKey("text")) {
                texto = String.valueOf(((Map<String, Object>) msg.get("text")).get("body"));
            } else if (msg.containsKey("button")) {
                texto = String.valueOf(((Map<String, Object>) msg.get("button")).get("text"));
            } else if (msg.containsKey("interactive")) {
                var interactive = (Map<String, Object>) msg.get("interactive");
                if (interactive.containsKey("button_reply")) {
                    texto = String.valueOf(((Map<String, Object>) interactive.get("button_reply")).get("title"));
                } else {
                    texto = String.valueOf(((Map<String, Object>) interactive.get("list_reply")).get("title"));
                }
            } else {
                texto = "";
            }
            return new Object[] { from, texto };
        } catch (Exception e) {
            log.warn("Payload Meta não reconhecido: {}", e.getMessage());
            return null;
        }
    }
}