package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.service.TransacaoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service){
        this.service = service;
    }

    @PostMapping("/transacoes")
    public Transacao salvar(@RequestBody Transacao transacao){
        return service.salvar(transacao);
    }

    @GetMapping("/transacoes")
    public List<Transacao> listar() {
        return service.listar();
    }
}
