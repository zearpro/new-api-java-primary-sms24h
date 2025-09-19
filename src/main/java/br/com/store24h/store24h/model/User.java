/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.Table
 *  javax.persistence.Inheritance
 *  javax.persistence.InheritanceType
 *  org.springframework.security.core.GrantedAuthority
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.security.core.userdetails.UserDetails
 */
package br.com.store24h.store24h.model;

import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.model.Role;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name="usuario", indexes={@Index(name="idx_email", columnList="email"), @Index(name="idx_apikey", columnList="apiKey")})
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class User
implements UserDetails,
Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String perfil;
    private String nome;
    private String cpf;
    private String senha;
    private String apiKey;
    private boolean whatsapp_enabled;
    private String callback_apiv2_id;
    private String tipo_de_api;
    private BigDecimal credito = BigDecimal.valueOf(0L);
    private BigDecimal agente_porcentagem_pagamento = BigDecimal.valueOf(100L);
    private String agente = null;

    public void setWhatsapp_enabled(boolean whatsapp_enabled) {
        this.whatsapp_enabled = whatsapp_enabled;
    }

    public boolean getWhatsAppEnabled() {
        return this.whatsapp_enabled;
    }

    public Long getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return this.nome;
    }

    public String getPerfil() {
        return this.perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return this.senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public BigDecimal getCredito() {
        return this.credito;
    }

    public void setCredito(BigDecimal credito) {
        this.credito = credito;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        HashSet<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(Role.USER.getNome()));
        return authorities;
    }

    public String getPassword() {
        return this.senha;
    }

    public String getUsername() {
        return this.email;
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    public TipoDeApiEnum getTipo_de_api() {
        if (this.tipo_de_api.equals("PADRAO")) {
            return TipoDeApiEnum.ANTIGA;
        }
        if (this.tipo_de_api.equals("ANTIGA")) {
            return TipoDeApiEnum.ANTIGA;
        }
        if (this.tipo_de_api.equals("SISTEMAS")) {
            return TipoDeApiEnum.SISTEMAS;
        }
        if (this.tipo_de_api.equals("SMSHUB")) {
            return TipoDeApiEnum.SMSHUB;
        }
        return TipoDeApiEnum.ANTIGA;
    }

    public void setTipo_de_api(TipoDeApiEnum tipo_de_api) {
        this.tipo_de_api = tipo_de_api.toString();
    }

    public String getCallbackUrlId() {
        return this.callback_apiv2_id;
    }

    public String getAgente() {
        return this.agente;
    }

    public BigDecimal getPorcentagemPagamento() {
        return this.agente_porcentagem_pagamento;
    }
}
