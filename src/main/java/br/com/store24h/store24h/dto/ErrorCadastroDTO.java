/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

public class ErrorCadastroDTO {
    private String msgError;

    public ErrorCadastroDTO(String msgError) {
        this.msgError = msgError;
    }

    public String getMsgError() {
        return this.msgError;
    }

    public void setMsgError(String msgError) {
        this.msgError = msgError;
    }
}
