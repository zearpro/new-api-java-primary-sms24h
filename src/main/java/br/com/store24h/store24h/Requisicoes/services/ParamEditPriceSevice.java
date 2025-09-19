/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.constraints.Max
 *  javax.validation.constraints.Positive
 */
package br.com.store24h.store24h.Requisicoes.services;

import java.math.BigDecimal;
import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;

public class ParamEditPriceSevice {
    @Positive
    @Max(value=1000L)
    private @Positive @Max(value=1000L) BigDecimal newPrice;

    public BigDecimal getNewPrice() {
        return this.newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }
}
