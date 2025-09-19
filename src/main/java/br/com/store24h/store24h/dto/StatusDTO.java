/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import br.com.store24h.store24h.model.Activation;

public class StatusDTO {
    private Long idActivation;
    private int status;

    public StatusDTO() {
    }

    public StatusDTO(Activation activation) {
        this.idActivation = activation.getId();
        this.status = activation.getStatus();
    }

    public Long getIdActivation() {
        return this.idActivation;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
