/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package br.com.store24h.store24h.apiv2;

import io.swagger.v3.oas.annotations.media.Schema;

public class BuyNumberDto {
    @Schema(description="API Key for authentication")
    private String apiKey;
    @Schema(description="Operator name", defaultValue="ANY")
    private String operator;
    @Schema(description="Service name")
    private String service;
    @Schema(description="country name", defaultValue="73")
    private String country;

    public BuyNumberDto() {
    }

    public BuyNumberDto(String apiKey, String operator, String service, String country) {
        this.apiKey = apiKey;
        this.operator = operator;
        this.service = service;
        this.country = country;
    }

    public String getOperator() {
        return this.operator.trim().equals("") ? "ANY" : this.operator.trim();
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
