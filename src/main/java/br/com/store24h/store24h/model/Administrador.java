/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import br.com.store24h.store24h.model.User;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
public class Administrador
extends User
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String operator;
    private String country;

    public Administrador() {
    }

    public Administrador(String operator, String country) {
        this.operator = operator;
        this.country = country;
    }

    public String getOperator() {
        return this.operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
