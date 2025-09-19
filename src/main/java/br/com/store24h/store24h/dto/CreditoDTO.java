/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import java.math.BigDecimal;

public class CreditoDTO {
    private BigDecimal credito;

    public CreditoDTO(BigDecimal credito) {
        this.credito = credito;
    }

    public BigDecimal getCredito() {
        return this.credito;
    }

    public void setCredito(BigDecimal credito) {
        this.credito = credito;
    }
}
