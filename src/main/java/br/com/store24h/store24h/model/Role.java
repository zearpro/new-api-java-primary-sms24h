/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.model;

public enum Role {
    USER("USER"),
    ADMINISTRADOR("ADMINISTRADOR");

    private String nome;

    private Role(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }
}
