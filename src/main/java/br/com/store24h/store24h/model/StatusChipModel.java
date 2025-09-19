/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.model;

public enum StatusChipModel {
    ACTIVITY(1),
    NOACTIVITY(0),
    INVALID(-1);

    private int status;

    private StatusChipModel(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }
}
