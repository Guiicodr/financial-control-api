package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IncomeService {
    private final IncomeRepository repository;
    private final UsuarioAtualService usuarioAtual;

    public IncomeService(IncomeRepository repository, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    public List<Income> listar() {
        return repository.findAllByUsuario(usuarioAtual.obter());
    }

    public Income salvar(Income income) {
        if (income.getDescricao() == null || income.getDescricao().isBlank())
            throw new IllegalArgumentException("Descrição é obrigatória");
        if (income.getValor() == null || income.getValor() <= 0)
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        income.setUsuario(usuarioAtual.obter());
        return repository.save(income);
    }

    public void deletar(Long id) {
        repository.findByIdAndUsuario(id, usuarioAtual.obter()).ifPresent(repository::delete);
    }
}
