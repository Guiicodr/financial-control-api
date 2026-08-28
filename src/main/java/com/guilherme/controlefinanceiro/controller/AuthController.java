package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Object registrar(@RequestBody Map<String, Object> body) {
        System.out.println(">>> PAYLOAD DE CADASTRO RECEBIDO: " + body);
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        String senha = (String) body.get("senha");
        if (senha == null)
            senha = (String) body.get("password"); // Fallback caso o front mande 'password'

        var usuario = service.registrar(name, email, senha);
        return Map.of("id", usuario.getId(), "name", usuario.getName(), "email", usuario.getEmail());
    }

    @PostMapping("/login")
    public Object login(@RequestBody Map<String, Object> body) {
        System.out.println(">>> PAYLOAD DE LOGIN RECEBIDO: " + body);
        String email = (String) body.get("email");
        String senha = (String) body.get("senha");
        if (senha == null)
            senha = (String) body.get("password"); // Fallback caso o front mande 'password'

        return service.autenticar(email, senha);
    }

    @PostMapping("/refresh")
    public AuthService.Resultado refresh(@RequestBody Map<String, String> body) {
        return service.renovar(body.get("refreshToken"));
    }
}