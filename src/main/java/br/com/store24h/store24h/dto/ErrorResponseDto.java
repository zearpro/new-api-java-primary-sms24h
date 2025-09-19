/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

public class ErrorResponseDto {
    private String msg = "Ops, algo deu errado!";

    public ErrorResponseDto() {
    }

    public ErrorResponseDto(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
