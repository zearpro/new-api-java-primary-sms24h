/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.response;

import br.com.store24h.store24h.dto.Dto;

public abstract class ServiceResponse<T extends Dto> {
    private String msg;

    public abstract T getDto();
}
