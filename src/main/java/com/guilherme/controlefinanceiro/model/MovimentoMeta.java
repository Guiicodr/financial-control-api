package com.guilherme.controlefinanceiro.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_movimento_usuario", columnList = "usuario_id"),
        @Index(name = "idx_movimento_objetivo", columnList = "objetivo_id")
})
public class MovimentoMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Objetivo objetivo;
    @ManyToOne(optional = false)
    private Usuario usuario;
    @Column(nullable = false)
    private Double valor;
    @Column(nullable = false)
    private LocalDateTime data = LocalDateTime.now();
    private String tipo;

    public MovimentoMeta() {
    }

    /**
     * Somente leitura no JSON: o movimento é criado sempre a partir do id do
     * objetivo que veio na requisição, e nunca de um id de movimento enviado
     * pelo cliente (que permitiria sobrescrever registros alheios).
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Long getId() {
        return id;
    }

    public Objetivo getObjetivo() {
        return objetivo;
    }

    public void setObjetivo(Objetivo objetivo) {
        this.objetivo = objetivo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
