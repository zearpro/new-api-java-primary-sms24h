/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.model;

public enum TimeZone {
    BR("America/Sao_Paulo");

    private String zone;

    private TimeZone(String zone) {
        this.zone = zone;
    }

    public String getZone() {
        return this.zone;
    }
}
