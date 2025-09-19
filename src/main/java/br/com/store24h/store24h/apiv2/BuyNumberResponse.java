/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.media.Schema$AccessMode
 */
package br.com.store24h.store24h.apiv2;

import io.swagger.v3.oas.annotations.media.Schema;

public class BuyNumberResponse {
    @Schema(example="11223355", description="Unique identifier for the purchase", accessMode=Schema.AccessMode.READ_ONLY)
    private String id;
    @Schema(example="5521988776655", description="Purchased phone number")
    private String number;
    @Schema(example="ot", description="Service associated with the purchase")
    private String service;

    public BuyNumberResponse() {
    }

    public BuyNumberResponse(String id, String number, String service) {
        this.id = id;
        this.number = number;
        this.service = service;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
