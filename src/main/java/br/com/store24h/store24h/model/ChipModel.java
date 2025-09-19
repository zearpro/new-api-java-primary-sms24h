/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="chip_model", indexes={@Index(name="idx_alugado", columnList="alugado"), @Index(name="idx_ativo", columnList="ativo")})
public class ChipModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    @Column(nullable=false)
    private String operadora;
    @Column(unique=true, nullable=false)
    private String number;
    private Boolean ativo = false;
    private Boolean alugado = false;
    private String pcId;
    private Boolean checked = false;
    private int status = 0;
    private String vendawhatsapp = "TODOS";

    public String getPcId() {
        return this.pcId;
    }

    public void setPcId(String pcId) {
        this.pcId = pcId;
    }

    public ChipModel(String number) {
        this.number = number;
    }

    public ChipModel() {
    }

    public String getVendawhatsapp() {
        return this.vendawhatsapp;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public void setVendawhatsapp(String venda) {
        this.vendawhatsapp = venda;
    }

    public Boolean getAlugado() {
        return this.alugado;
    }

    public void setAlugado(Boolean alugado) {
        this.alugado = alugado;
    }

    public long getId() {
        return this.id;
    }

    public String getOperadora() {
        return this.operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public String getNumber() {
        return this.number;
    }

    public int getStatus() {
        return this.status;
    }

    public Boolean getChecked() {
        return this.checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String toString() {
        return "ChipModel{operadora=" + this.operadora + ", number=" + this.number + "}";
    }

    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.number);
        return hash;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ChipModel other = (ChipModel)obj;
        return Objects.equals(this.number, other.number);
    }
}
