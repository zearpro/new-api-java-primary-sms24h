/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.apiv2.model;

public class ChipNumberControlAliasServiceDTO {
    private Long chipNumberControlId;
    private String aliasService;
    private String numero;

    public ChipNumberControlAliasServiceDTO(Long chipNumberControlId, String aliasService, String numero) {
        this.chipNumberControlId = chipNumberControlId;
        this.aliasService = aliasService;
        this.numero = numero;
    }

    public Long getChipNumberControlId() {
        return this.chipNumberControlId;
    }

    public String getAliasService() {
        return this.aliasService;
    }

    public String getNumero() {
        return this.numero;
    }
}
