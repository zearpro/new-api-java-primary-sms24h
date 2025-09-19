/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.constraints.NotBlank
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 */
package br.com.store24h.store24h.Requisicoes;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.model.Role;
import br.com.store24h.store24h.model.User;
import javax.validation.constraints.NotBlank;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class RequisicaoNovoUser {
    @NotBlank
    private String nome;
    @NotBlank
    private String cpf;
    @NotBlank
    private String email;
    @NotBlank
    private String senhaUser;
    @NotBlank
    private String senhaUser2;
    private String apiKey;
    private int saldo;

    public RequisicaoNovoUser() {
    }

    public RequisicaoNovoUser(User user) {
        this.nome = user.getNome();
        this.cpf = user.getCpf();
        this.email = user.getEmail();
    }

    public RequisicaoNovoUser(String nome, String cpf, String email, String senhaUser, String senhaUser2, String apiKey, int saldo) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.senhaUser = senhaUser;
        this.senhaUser2 = senhaUser2;
        this.apiKey = apiKey;
        this.saldo = saldo;
    }

    public String getNome() {
        return this.nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaUser() {
        return this.senhaUser;
    }

    public void setSenhaUser(String senhaUser) {
        this.senhaUser = senhaUser;
    }

    public String getSenhaUser2() {
        return this.senhaUser2;
    }

    public void setSenhaUser2(String senhaUser2) {
        this.senhaUser2 = senhaUser2;
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

    public User toUser(Funcionalidades funcionalidades) {
        User user = new User();
        user.setNome(this.nome);
        user.setApiKey(funcionalidades.gerarKeyApi(this.email));
        user.setCpf(this.cpf);
        user.setEmail(this.email);
        user.setPerfil(Role.USER.name());
        user.setSenha(new BCryptPasswordEncoder().encode((CharSequence)this.senhaUser));
        return user;
    }
}
