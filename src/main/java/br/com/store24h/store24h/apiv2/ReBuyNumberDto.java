/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package br.com.store24h.store24h.apiv2;

import io.swagger.v3.oas.annotations.media.Schema;

public class ReBuyNumberDto {
    @Schema(description="API Key for authentication")
    private String apiKey;
    @Schema(description="Activation ID", defaultValue="833737")
    private String activation_id;
    @Schema(description="Country code")
    private String country;

    public ReBuyNumberDto() {
    }

    public ReBuyNumberDto(String apiKey, String activation_id) {
        this.apiKey = apiKey;
        this.activation_id = activation_id;
    }

    public ReBuyNumberDto(String apiKey, String activation_id, String country) {
        this.apiKey = apiKey;
        this.activation_id = activation_id;
        this.country = country;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getActivation_id() {
        return this.activation_id;
    }

    public void setActivation_id(String activation_id) {
        this.activation_id = activation_id;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
