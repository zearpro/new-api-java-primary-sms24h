/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ComprasCredito
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime localDateTime;
    private BigDecimal valorComprado;

    public ComprasCredito() {
    }

    public ComprasCredito(LocalDateTime localDateTime, BigDecimal valorComprado) {
        this.localDateTime = localDateTime;
        this.valorComprado = valorComprado;
    }

    public Long getId() {
        return this.id;
    }

    public LocalDateTime getLocalDateTime() {
        return this.localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public BigDecimal getValorComprado() {
        return this.valorComprado;
    }

    public void setValorComprado(BigDecimal valorComprado) {
        this.valorComprado = valorComprado;
    }
}
