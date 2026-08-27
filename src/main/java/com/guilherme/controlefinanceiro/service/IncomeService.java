package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Income;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;
import com.guilherme.controlefinanceiro.model.TipoRenda;

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
        var usuario = usuarioAtual.obter();
        TipoRenda tipo = income.getTipo() == null ? TipoRenda.EXTRA : income.getTipo();
        income.setTipo(tipo);
        income.setData(income.getData() == null ? LocalDate.now() : income.getData());
        income.setUsuario(usuario);
        if (tipo == TipoRenda.BASE) {
            repository.findByUsuarioAndTipo(usuario, TipoRenda.BASE)
                    .filter(base -> !base.getId().equals(income.getId()))
                    .ifPresent(base -> { throw new IllegalArgumentException("O usuário já possui uma renda base"); });
        }
        return repository.save(income);
    }

    public Income salvarRendaBase(Income income) {
        income.setTipo(TipoRenda.BASE);
        var usuario = usuarioAtual.obter();
        repository.findByUsuarioAndTipo(usuario, TipoRenda.BASE).ifPresent(base -> {
            income.setId(base.getId());
            if (income.getData() == null) {
                income.setData(base.getData());
            }
        });
        return salvar(income);
    }

    public Income salvarRendaExtra(Income income) {
        income.setTipo(TipoRenda.EXTRA);
        return salvar(income);
    }

    public double totalNoMes(YearMonth mes) {
        return listar().stream().mapToDouble(renda -> valorNoMes(renda, mes)).sum();
    }

    public double totalAcumuladoAte(YearMonth mes) {
        return listar().stream().mapToDouble(renda -> {
            if (renda.getTipo() != TipoRenda.BASE || renda.getData() == null) {
                // Registros criados antes desta mudança não tinham data/tipo.
                // Eles representam uma renda pontual já existente no saldo.
                return renda.getData() == null || !YearMonth.from(renda.getData()).isAfter(mes) ? renda.getValor() : 0;
            }
            YearMonth inicio = YearMonth.from(renda.getData());
            long quantidadeDeMeses = inicio.until(mes, java.time.temporal.ChronoUnit.MONTHS) + 1;
            return quantidadeDeMeses > 0 ? renda.getValor() * quantidadeDeMeses : 0;
        }).sum();
    }

    public double valorNoMes(Income renda, YearMonth mes) {
        if (renda.getValor() == null) return 0;
        if (renda.getData() == null) return YearMonth.now().equals(mes) ? renda.getValor() : 0;
        if (renda.getTipo() == TipoRenda.BASE) return YearMonth.from(renda.getData()).isAfter(mes) ? 0 : renda.getValor();
        return YearMonth.from(renda.getData()).equals(mes) ? renda.getValor() : 0;
    }

    public void deletar(Long id) {
        repository.findByIdAndUsuario(id, usuarioAtual.obter()).ifPresent(repository::delete);
    }
}
