package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.MovimentoMeta;
import com.guilherme.controlefinanceiro.service.MovimentoMetaService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/metas/movimentos")
public class MovimentoMetaController {
    private final MovimentoMetaService service;

    public MovimentoMetaController(MovimentoMetaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MovimentoMeta> listar() {
        return service.listar();
    }

    @PostMapping("/{objetivoId}")
    public MovimentoMeta registrar(@PathVariable Long objetivoId, @RequestBody MovimentoMeta movimento) {
        return service.registrar(objetivoId, movimento);
    }
}
