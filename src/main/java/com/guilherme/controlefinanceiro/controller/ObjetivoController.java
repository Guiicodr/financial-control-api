package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Objetivo;
import com.guilherme.controlefinanceiro.service.ObjetivoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/objetivos")
public class ObjetivoController {

    private final ObjetivoService service;

    public ObjetivoController(ObjetivoService service) {
        this.service = service;
    }

    @PostMapping
    public Objetivo salvar(@RequestBody Objetivo objetivo) {
        return service.salvar(objetivo);
    }

    @GetMapping
    public List<Objetivo> listar() {
        return service.listar();
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}