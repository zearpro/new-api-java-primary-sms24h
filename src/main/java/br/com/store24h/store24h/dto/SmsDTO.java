/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.dto;

import java.util.ArrayList;
import java.util.List;

public class SmsDTO {
    private String nameService;
    private int status;
    private String aliasService;
    private String numberActivation;
    private List<String> smsList = new ArrayList<String>();

    public String getNameService() {
        return this.nameService;
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

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
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
}
