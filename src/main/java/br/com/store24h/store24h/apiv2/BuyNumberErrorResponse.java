/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.apiv2;

public class BuyNumberErrorResponse {
    String status;
    String error;

    public BuyNumberErrorResponse(String status, String error) {
        this.status = status;
        this.error = error;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
