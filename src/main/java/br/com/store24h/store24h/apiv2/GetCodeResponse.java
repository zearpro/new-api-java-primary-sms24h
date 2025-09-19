/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonIgnore
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.media.Schema$AccessMode
 */
package br.com.store24h.store24h.apiv2;

import br.com.store24h.store24h.apiv2.ActionsPermitedEnum;
import br.com.store24h.store24h.apiv2.CodeResponseStatusEnum;
import br.com.store24h.store24h.model.ActivationStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;

public class GetCodeResponse {
    @Schema(accessMode=Schema.AccessMode.READ_ONLY)
    private long id = 0L;
    private CodeResponseStatusEnum error;
    private ActivationStatusEnum status;
    private String code;
    private String number;
    private String service;
    private int statusInt;
    private ArrayList<ActionsPermitedEnum> actionsPermited = new ArrayList();

    public GetCodeResponse(long id, String service, String number, ActivationStatusEnum status, CodeResponseStatusEnum error, String code) {
        this.status = status;
        this.code = code;
        this.number = number;
        this.service = service.split("_")[0];
        this.error = error;
        this.id = id;
    }

    public GetCodeResponse() {
        this.status = null;
        this.code = null;
        this.number = null;
        this.service = null;
        this.error = null;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service.split("_")[0];
    }

    public String getStatus() {
        return this.status == null ? (this.statusInt > 0 ? String.valueOf(this.statusInt) : null) : this.status.name();
    }

    @Schema(accessMode=Schema.AccessMode.READ_ONLY)
    @JsonIgnore
    public ActivationStatusEnum getStatusEnum() {
        return this.status;
    }

    public void setStatus(ActivationStatusEnum status, int statusInt) {
        this.status = status;
        this.statusInt = statusInt;
        ArrayList<ActionsPermitedEnum> actionsPermited = new ArrayList<ActionsPermitedEnum>();
        if (status.equals((Object)ActivationStatusEnum.STATUS_OK)) {
            actionsPermited = new ArrayList<ActionsPermitedEnum>(Arrays.asList(ActionsPermitedEnum.FINISH, ActionsPermitedEnum.RETRY));
        } else if (status.equals((Object)ActivationStatusEnum.STATUS_WAIT_CODE)) {
            actionsPermited = new ArrayList<ActionsPermitedEnum>(Arrays.asList(ActionsPermitedEnum.CANCEL));
        } else if (status.equals((Object)ActivationStatusEnum.STATUS_WAIT_RETRY)) {
            actionsPermited = new ArrayList<ActionsPermitedEnum>(Arrays.asList(ActionsPermitedEnum.FINISH));
        }
        this.actionsPermited = actionsPermited;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public CodeResponseStatusEnum getError() {
        return this.error;
    }

    public void setError(CodeResponseStatusEnum error) {
        this.error = error;
    }

    public void setId(Long aLong) {
        this.id = aLong;
    }

    public ArrayList<ActionsPermitedEnum> getActionsPermited() {
        return this.actionsPermited;
    }
}
