/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import java.util.ArrayList;
import java.util.List;

public class ActivationsDTO {
    private Long idUser;
    private Long idActivation;
    private String status;
    private String nameService;
    private String aliasService;
    private Boolean retry;
    private Boolean awaitSms;
    private String min;
    private boolean finalized = false;
    private String numberActivation;
    private List<String> smsList = new ArrayList<String>();

    public Long getIdUser() {
        return this.idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public Long getIdActivation() {
        return this.idActivation;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public String getNameService() {
        return this.nameService;
    }

    public String getStatus() {
        return this.status;
    }

    public String getMin() {
        return this.min;
    }

    public Boolean getRetry() {
        return this.retry;
    }

    public void setRetry(Boolean retry) {
        this.retry = retry;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setNameService(String nameService) {
        this.nameService = nameService;
    }

    public String getAliasService() {
        return this.aliasService;
    }

    public void setAliasService(String aliasService) {
        this.aliasService = aliasService;
    }

    public String getNumberActivation() {
        return this.numberActivation;
    }

    public void setNumberActivation(String numberActivation) {
        this.numberActivation = numberActivation;
    }

    public List<String> getSmsList() {
        return this.smsList;
    }

    public void setSmsList(List<String> smsList) {
        this.smsList = smsList;
    }

    public boolean isFinalized() {
        return this.finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public Boolean getAwaitSms() {
        return this.awaitSms;
    }

    public void setAwaitSms(Boolean awaitSms) {
        this.awaitSms = awaitSms;
    }
}
