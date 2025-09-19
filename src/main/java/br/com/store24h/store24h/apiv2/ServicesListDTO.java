/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.apiv2;

public class ServicesListDTO {
    private final String name;
    private final String code;

    public ServicesListDTO(String name, String alias) {
        this.name = name;
        this.code = alias;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }
}
