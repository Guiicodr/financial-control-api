package com.guilherme.controlefinanceiro.model;

import jakarta.persistence.*;

import java.time.LocalDate;

// Modelo dos dados financeiros enviados
@Entity
public class Transacao {

    // Organização ordenada dos valores alocados
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Descrição do que foi gasto ou objetivos
    private String descricao;

    // Entrando ou saindo
    private String tipo;

    // Valor enviado
    private Double valor;

    // Data de entradas, prazos de objetivo
    private LocalDate data;

    private Integer parcelaAtual;
    private Integer totalParcelas;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne
    private CartaoCredito cartaoCredito;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    // Getters e Setters
    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParcelaAtual() {
        return parcelaAtual;
    }

    public void setParcelaAtual(Integer parcelaAtual) {
        this.parcelaAtual = parcelaAtual;
    }

    public Integer getTotalParcelas() {
        return totalParcelas;
    }

    public void setTotalParcelas(Integer totalParcelas) {
        this.totalParcelas = totalParcelas;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public CartaoCredito getCartaoCredito() {
        return cartaoCredito;
    }

    public void setCartaoCredito(CartaoCredito cartaoCredito) {
        this.cartaoCredito = cartaoCredito;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

}
