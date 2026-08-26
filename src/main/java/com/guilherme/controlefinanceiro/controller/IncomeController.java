package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.service.IncomeService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/income")
public class IncomeController {
    private final IncomeService service;

    public IncomeController(IncomeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Income> listar() {
        return service.listar();
    }

    @PostMapping
    public Income salvar(@RequestBody Income income) {
        return service.salvar(income);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}
