package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.model.CartaoCredito;
import com.guilherme.controlefinanceiro.repository.CartaoCreditoRepository;
import com.guilherme.controlefinanceiro.service.UsuarioAtualService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/cartoes")
public class CartaoCreditoController {
    private final CartaoCreditoRepository repository;
    private final UsuarioAtualService usuarioAtual;

    public CartaoCreditoController(CartaoCreditoRepository repository, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public List<CartaoCredito> listar() {
        return repository.findAllByUsuario(usuarioAtual.obter());
    }

    @PostMapping
    public CartaoCredito salvar(@RequestBody CartaoCredito cartao) {
        cartao.setUsuario(usuarioAtual.obter());
        return repository.save(cartao);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.findById(id).filter(item -> item.getUsuario().getId().equals(usuarioAtual.obter().getId()))
                .ifPresent(repository::delete);
    }
}
