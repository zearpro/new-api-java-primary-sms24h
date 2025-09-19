/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import br.com.store24h.store24h.model.User;

public class UserDTO {
    private String nome;
    private String email;
    private String cpf;
    private String apiKey;
    private String role;
    private int saldo;

    public UserDTO() {
    }

    public UserDTO(User user) {
        this.nome = user.getNome();
        this.email = user.getEmail();
        this.cpf = user.getCpf();
        this.apiKey = user.getApiKey();
        this.role = user.getPerfil();
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return this.email;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getSaldo() {
        return this.saldo;
    }

    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }
}
