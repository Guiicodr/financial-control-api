package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.MovimentoMeta;
import com.guilherme.controlefinanceiro.repository.MovimentoMetaRepository;
import com.guilherme.controlefinanceiro.repository.ObjetivoRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MovimentoMetaService {
    private final MovimentoMetaRepository movimentos;
    private final ObjetivoRepository objetivos;
    private final UsuarioAtualService usuarioAtual;

    public MovimentoMetaService(MovimentoMetaRepository movimentos, ObjetivoRepository objetivos,
            UsuarioAtualService usuarioAtual) {
        this.movimentos = movimentos;
        this.objetivos = objetivos;
        this.usuarioAtual = usuarioAtual;
    }

    public MovimentoMeta registrar(Long objetivoId, MovimentoMeta movimento) {
        var usuario = usuarioAtual.obter();
        var objetivo = objetivos.findById(objetivoId).filter(item -> item.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow();
        if (movimento.getValor() == null || movimento.getValor() <= 0)
            throw new IllegalArgumentException("Valor inválido");
        movimento.setObjetivo(objetivo);
        movimento.setUsuario(usuario);
        movimento.setTipo(movimento.getTipo() == null ? "APORTE" : movimento.getTipo());
        objetivo.setValorAtual(Math.max(0, objetivo.getValorAtual()
                + ("RESGATE".equals(movimento.getTipo()) ? -movimento.getValor() : movimento.getValor())));
        objetivos.save(objetivo);
        return movimentos.save(movimento);
    }

    public List<MovimentoMeta> listar() {
        return movimentos.findAllByUsuario(usuarioAtual.obter());
    }
}
