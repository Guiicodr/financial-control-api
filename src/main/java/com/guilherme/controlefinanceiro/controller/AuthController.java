package com.guilherme.controlefinanceiro.controller;
import com.guilherme.controlefinanceiro.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@RequestBody Map<String, Object> body) {
        // Aceita "name" (padrão) e "nome" (compatibilidade com versões antigas do front)
        String name = body.get("name") != null ? (String) body.get("name") : (String) body.get("nome");
        String email = (String) body.get("email");
        String senha = (String) body.get("senha");
        if (senha == null)
            senha = (String) body.get("password"); // Fallback caso o front mande 'password'

        if (email == null || !EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("E-mail inválido");
        if (senha == null || senha.length() < 6)
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");

        var usuario = service.registrar(name, email, senha);
        return Map.of("id", usuario.getId(), "name", usuario.getName(), "email", usuario.getEmail());
    }

    @PostMapping("/login")
    public AuthService.Resultado login(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String senha = (String) body.get("senha");
        if (senha == null)
            senha = (String) body.get("password"); // Fallback caso o front mande 'password'

        if (email == null || !EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("E-mail inválido");

        return service.autenticar(email, senha);
    }

    @PostMapping("/refresh")
    public AuthService.Resultado refresh(@RequestBody Map<String, String> body) {
        return service.renovar(body.get("refreshToken"));
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches())
            throw new IllegalArgumentException("E-mail inválido");
        String token = service.solicitarResetSenha(email);
        return Map.of(
            "mensagem", "Link de recuperação enviado para seu e-mail (em produção). Token de teste: " + token,
            "token", token
        );
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String senha = body.get("senha");
        if (token == null || token.isBlank())
            throw new IllegalArgumentException("Token é obrigatório");
        if (senha == null || senha.length() < 6)
            throw new IllegalArgumentException("Senha deve ter no mínimo 6 caracteres");
        service.resetarSenha(token, senha);
        return Map.of("mensagem", "Senha redefinida com sucesso");
    }
}
