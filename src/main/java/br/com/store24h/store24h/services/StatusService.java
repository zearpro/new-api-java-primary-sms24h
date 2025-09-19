/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.services;

public enum StatusService {
    SOLICITADA(2),
    AGUARDANDO_MENSAGENS(3),
    FINALIZADA(5),
    CANCELADA(7);


    private StatusService(int i) {
    }
}
