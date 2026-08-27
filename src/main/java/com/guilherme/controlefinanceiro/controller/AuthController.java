package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@RequestBody AuthRequest request) {
        var usuario = service.registrar(request.name(), request.email(), request.senha());
        return new UsuarioResponse(usuario.getId(), usuario.getName(), usuario.getEmail());
    }

    @PostMapping("/login")
    public AuthService.Resultado login(@RequestBody AuthRequest request) {
        return service.autenticar(request.email(), request.senha());
    }

    @PostMapping("/refresh")
    public AuthService.Resultado refresh(@RequestBody RefreshRequest request) {
        return service.renovar(request.refreshToken());
    }

    public record AuthRequest(String name, String email, String senha) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record UsuarioResponse(Long id, String name, String email) {
    }
}
