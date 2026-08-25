package com.guilherme.controlefinanceiro.model;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "categoria" }))
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;
    @Column(nullable = false)
    private Double limiteMensal;
    @ManyToOne(optional = false)
    private Usuario usuario;

    public Orcamento() {
    }

    public Long getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Double getLimiteMensal() {
        return limiteMensal;
    }

    public void setLimiteMensal(Double limiteMensal) {
        this.limiteMensal = limiteMensal;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
