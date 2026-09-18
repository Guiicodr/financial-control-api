package com.guilherme.controlefinanceiro.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento global de exceções da API.
 *
 * Antes deste handler, {@link IllegalArgumentException} (validação de entrada
 * em AuthService/TransacaoService/etc.) e {@link IllegalStateException}
 * (sessão inválida em UsuarioAtualService) chegavam ao cliente como HTTP 500
 * com stacktrace — o front exibia "Erro (500)" e não havia como distinguir
 * erro de input de erro de servidor. Aqui cada família de exceção vira um
 * status semântico, com corpo JSON curto e sem detalhe interno.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /** Entrada inválida do cliente: e-mail malformado, valor <= 0, campo obrigatório... */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Requisição inválida");
    }

    /** Corpo ausente ou JSON malformado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Corpo de requisição inválido: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.");
    }

    /** Falha de validação de @Valid em DTOs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .orElse("Dados inválidos.");
        return build(HttpStatus.BAD_REQUEST, mensagem);
    }

    /** Sessão ausente/expirada ou usuário do token removido (UsuarioAtualService). */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(IllegalStateException ex) {
        log.debug("Sessão inválida: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada. Faça login novamente.");
    }

    /** Registro de outro usuário / acesso negado por papel. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    /** Rede de segurança: nunca devolver stacktrace ao cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Erro inesperado na API", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", status.getReasonPhrase());
        corpo.put("message", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}