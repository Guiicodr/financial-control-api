package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.dto.ProjecaoMensalDTO;
import com.guilherme.controlefinanceiro.service.ProjecaoService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projecoes")
public class ProjecaoController {
    private final ProjecaoService service;

    public ProjecaoController(ProjecaoService service) {
        this.service = service;
    }

    @GetMapping("/saldo")
    public List<ProjecaoMensalDTO> saldo(@RequestParam(defaultValue = "6") int meses) {
        return service.projetar(meses);
    }
}
