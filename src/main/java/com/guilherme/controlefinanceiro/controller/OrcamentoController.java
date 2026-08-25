package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Orcamento;
import com.guilherme.controlefinanceiro.service.OrcamentoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {
    private final OrcamentoService service;

    public OrcamentoController(OrcamentoService service) {
        this.service = service;
    }

    @PostMapping
    public Orcamento salvar(@RequestBody Orcamento item) {
        return service.salvar(item);
    }

    @GetMapping("/alertas")
    public List<Map<String, Object>> alertas() {
        return service.alertas();
    }
}
