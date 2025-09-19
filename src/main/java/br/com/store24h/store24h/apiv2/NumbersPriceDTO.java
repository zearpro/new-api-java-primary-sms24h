/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.apiv2;

import br.com.store24h.store24h.apiv2.ServicesListDTO;
import java.math.BigDecimal;

public class NumbersPriceDTO
extends ServicesListDTO {
    private final BigDecimal price;

    public NumbersPriceDTO(String name, String alias, BigDecimal price) {
        super(name, alias);
        this.price = price;
    }

    public BigDecimal getPrice() {
        return this.price;
    }
}
