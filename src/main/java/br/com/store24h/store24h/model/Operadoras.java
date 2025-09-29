/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="v_operadoras")
public class Operadoras
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    private String operadora;
    @Column(name="country", nullable=true)
    private String country;

    public Operadoras(String operadora) {
        this.operadora = operadora;
    }

    public Operadoras() {
    }

    public String getOperadora() {
        return this.operadora;
    }

    public void setOperadora(String operadora) {
        this.operadora = operadora;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
