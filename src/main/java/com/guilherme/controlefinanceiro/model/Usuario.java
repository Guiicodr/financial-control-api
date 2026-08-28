package com.guilherme.controlefinanceiro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Mantido nullable na migração automática para não impedir o deploy em bancos
    // que já possuem usuários. Novos cadastros são validados pelo AuthService.
    @Column
    private String name;

    @Column(nullable = false)
    private String senha;

    // Número do WhatsApp (apenas dígitos) vinculado à conta para lançamentos via chat
    @Column
    private String telefone;

    public Usuario() {
    }

    public Usuario(String name, String email, String senha) {
        this.name = name;
        this.email = email;
        this.senha = senha;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
