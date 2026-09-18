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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * Segurança: defina WHATSAPP_WEBHOOK_TOKEN no Railway e configure o mesmo valor
 * no provedor (header X-Webhook-Token ou query ?token=...). O endpoint é
 * PÚBLICO no SecurityFilterChain, então o token é a única barreira: sem ele
 * configurado, esta API responde 401 (fail-closed) em vez de aceitar qualquer
 * POST anônimo — que permitiria lançar despesas na conta de outra pessoa
 * apenas sabendo o telefone dela.
 */
@RestController
public class WhatsAppWebhookController {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppService service;
    private final String webhookToken;
    private final String metaToken;
    private final String metaPhoneNumberId;
    private final HttpClient http = HttpClient.newHttpClient();

    public WhatsAppWebhookController(WhatsAppService service,
            @Value("${app.whatsapp.webhook-token:}") String webhookToken,
            @Value("${app.whatsapp.meta.token:}") String metaToken,
            @Value("${app.whatsapp.meta.phone-number-id:}") String metaPhoneNumberId) {
        this.service = service;
        this.webhookToken = webhookToken == null ? "" : webhookToken;
        this.metaToken = metaToken == null ? "" : metaToken;
        this.metaPhoneNumberId = metaPhoneNumberId == null ? "" : metaPhoneNumberId;
    }

    /**
     * Valida o token do provedor de forma fail-closed: se o servidor não tem
     * WHATSAPP_WEBHOOK_TOKEN configurado, NENHUMA chamada é aceita. A comparação
     * usa {@link MessageDigest#isEqual} para não vazar o token por timing attack.
     */
    private boolean tokenValido(String headerToken, Map<String, String> form) {
        if (webhookToken.isBlank()) {
            return false;
        }
        String queryToken = form == null ? null : form.get("token");
        return iguais(webhookToken, headerToken) || iguais(webhookToken, queryToken);
    }

    private boolean iguais(String esperado, String recebido) {
        if (recebido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                recebido.getBytes(StandardCharsets.UTF_8));
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
        // apenas ecoe o challenge (modo de teste). O challenge é apenas um eco do
        // valor enviado pela Meta, por isso não é um vazamento de dados.
        String verifyToken = System.getenv().getOrDefault("WHATSAPP_VERIFY_TOKEN", "");
        if (verifyToken.isBlank()) {
            log.warn("WHATSAPP_VERIFY_TOKEN não configurado: verificação da Meta aceita sem validação.");
        } else if (!verifyToken.equals(hubVerifyToken)) {
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

        if (!tokenValido(headerToken, form)) {
            log.warn("Webhook do WhatsApp rejeitado: token ausente ou inválido "
                    + "(configure WHATSAPP_WEBHOOK_TOKEN no servidor e no provedor).");
            return ResponseEntity.status(401).body("token inválido");
        }

        try {
            if (json != null && !json.isEmpty()) {
                return processarMeta(json);
            }
            if (form != null && !form.isEmpty()) {
                String from = form.getOrDefault("From", form.getOrDefault("WaId", ""));
                String body = form.getOrDefault("Body", form.getOrDefault("Texto", ""));
                return responderTwilio(from, body);
            }
        } catch (Exception e) {
            // Nunca derrube o webhook: provedores desativam endpoints que falham.
            log.error("Falha ao processar mensagem do WhatsApp", e);
        }

        return ResponseEntity.ok("ok");
    }

    /**
     * Twilio: a resposta precisa ser TwiML XML para o bot enviar a mensagem
     * de volta no chat.
     */
    private ResponseEntity<String> responderTwilio(String telefone, String mensagem) {
        String resposta = service.processarMensagem(telefone, mensagem);
        String xml = "<Response><Message>" + escaparXml(resposta) + "</Message></Response>";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(xml);
    }

    private ResponseEntity<String> processarMeta(Map<String, Object> json) {
        Object[] dados = extrairMeta(json);
        if (dados != null && dados.length >= 2) {
            String resposta = service.processarMensagem((String) dados[0], (String) dados[1]);
            enviarMeta((String) dados[0], resposta);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /**
     * Meta Cloud API: o webhook não devolve a mensagem no corpo da resposta —
     * é preciso chamá-la de volta via Graph API com o token do app.
     */
    private void enviarMeta(String to, String texto) {
        if (metaToken.isBlank() || metaPhoneNumberId.isBlank()) {
            log.warn("WHATSAPP_TOKEN/WHATSAPP_PHONE_NUMBER_ID não configurados: resposta não enviada via Meta Cloud API");
            return;
        }
        try {
            String payload = payloadMeta(to, texto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/v20.0/" + metaPhoneNumberId + "/messages"))
                    .header("Authorization", "Bearer " + metaToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Meta Cloud API respondeu {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Falha ao enviar resposta via Meta Cloud API", e);
        }
    }

    /** Monta o JSON de envio de texto da Meta Cloud API. */
    private String payloadMeta(String to, String texto) {
        String body = texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "{\"messaging_product\":\"whatsapp\",\"to\":\"" + to
                + "\",\"type\":\"text\",\"text\":{\"body\":\"" + body + "\"}}";
    }

    /**
     * Escapa caracteres especiais para uso seguro dentro de TwiML/XML.
     * As entidades são montadas por concatenação para ficarem explícitas.
     */
    private String escaparXml(String texto) {
        String amp = "&" + "amp;";
        String lt = "&" + "lt;";
        String gt = "&" + "gt;";
        String quot = "&" + "quot;";
        String apos = "&" + "apos;";
        return texto.replace("&", amp)
                .replace("<", lt)
                .replace(">", gt)
                .replace("\"", quot)
                .replace("'", apos);
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