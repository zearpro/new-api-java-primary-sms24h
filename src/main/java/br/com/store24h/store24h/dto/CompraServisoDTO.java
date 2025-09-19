/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import br.com.store24h.store24h.model.CompraServiso;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompraServisoDTO {
    private Long idActivation;
    private LocalDateTime localDateTime;
    private String aliasService;
    private String number;
    private String sms = "";
    private BigDecimal cost;
    private int status = 1;
    private Long idUser;
    private Long totalElements;
    private int totalPages;
    private int numberOfElements;

    public CompraServisoDTO(CompraServiso compraServiso, Long totalElements, int totalPages, int numberOfElements) {
        this.idActivation = compraServiso.getIdActivation();
        this.localDateTime = compraServiso.getLocalDateTime();
        this.aliasService = compraServiso.getAliasService();
        this.number = compraServiso.getNumber();
        this.sms = compraServiso.getSms();
        this.cost = compraServiso.getCost();
        this.status = compraServiso.getStatus();
        this.idUser = compraServiso.getIdUser();
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.numberOfElements = numberOfElements;
    }

    public Long getIdActivation() {
        return this.idActivation;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public LocalDateTime getLocalDateTime() {
        return this.localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getAliasService() {
        return this.aliasService;
    }

    public void setAliasService(String aliasService) {
        this.aliasService = aliasService;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSms() {
        return this.sms;
    }

    public void setSms(String sms) {
        this.sms = sms;
    }

    public BigDecimal getCost() {
        return this.cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Long getIdUser() {
        return this.idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public Long getTotalElements() {
        return this.totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumberOfElements() {
        return this.numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }
}
