/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.apiv2;

import br.com.store24h.store24h.apiv2.ServicesListDTO;

public class NumbersDisponibleDTO
extends ServicesListDTO {
    int quantity;

    public NumbersDisponibleDTO(String name, String code, int quantity) {
        super(name, code);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
