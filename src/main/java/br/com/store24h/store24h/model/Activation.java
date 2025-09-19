/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.CollectionTable
 *  javax.persistence.Column
 *  javax.persistence.ElementCollection
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.JoinColumn
 *  javax.validation.constraints.NotBlank
 *  org.hibernate.annotations.Fetch
 *  org.hibernate.annotations.FetchMode
 */
package br.com.store24h.store24h.model;

import br.com.store24h.store24h.model.ActivationStatusEnum;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.TimeZone;
import br.com.store24h.store24h.services.core.ActivationStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.validation.constraints.NotBlank;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
public class Activation
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    @NotBlank
    private String serviceName;
    @NotBlank
    private String aliasService;
    private String apiKey;
    private String serviceNumber;
    private BigDecimal servicePrice;
    private String chipNumber;
    @Column(columnDefinition = "TINYINT")
    private ActivationStatus statusBuz = ActivationStatus.SOLICITADA;
    @ElementCollection
    @CollectionTable(name="sms_string_models", joinColumns={@JoinColumn(name="activation_id")})
    @Column(name="sms_string_models")
    @Fetch(value=FetchMode.JOIN)
    private final List<String> smsStringModels = new ArrayList<String>();
    private Integer neededSmsToFinalize = 1;
    private LocalDateTime initialTime = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone()));
    private LocalDateTime endTime;
    private int status = -1;
    private int visible;
    private int version;
    private String receivedToFinishTime;

    public Activation() {
    }

    public Activation(Servico service, String chipNumber, String apiKey, int version, int visible) {
        this.aliasService = service.getAlias();
        this.serviceName = service.getName();
        this.servicePrice = service.getPrice();
        this.chipNumber = chipNumber;
        this.apiKey = apiKey;
        this.version = version;
        this.visible = visible;
    }

    public Activation(String serviceName, String chipNumber) {
        this.serviceName = serviceName;
        this.chipNumber = chipNumber;
    }

    public List<String> getSmsStringModels() {
        return this.smsStringModels;
    }

    public boolean reserveNumber() {
        boolean resp = false;
        this.setInitialTime(LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())));
        resp = true;
        return resp;
    }

    public boolean cancelService() {
        this.statusBuz = ActivationStatus.CANCELADA;
        return true;
    }

    public String toString() {
        return "Activation{serviceName='" + this.serviceName + "', serviceNumber='" + this.serviceNumber + "', chipNumber='" + this.chipNumber + "', statusBuz=" + this.statusBuz + ", smsStringModels=" + this.smsStringModels + ", neededSmsToFinalize=" + this.neededSmsToFinalize + ", initialTime=" + this.initialTime + ", endTime=" + this.endTime + ", status=" + this.status + "}";
    }

    public String getServiceNumber() {
        return this.serviceNumber;
    }

    public void setServiceNumber(String serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setStatusBuz(ActivationStatus statusBuz) {
        this.statusBuz = statusBuz;
    }

    public long getId() {
        return this.id;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getAliasService() {
        return this.aliasService;
    }

    public void setAliasService(String aliasService) {
        this.aliasService = aliasService;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public BigDecimal getServicePrice() {
        return this.servicePrice;
    }

    public void setServicePrice(BigDecimal servicePrice) {
        this.servicePrice = servicePrice;
    }

    public String getChipNumber() {
        return this.chipNumber;
    }

    public ActivationStatus getStatusBuz() {
        return this.statusBuz;
    }

    public Integer getNeededSmsToFinalize() {
        return this.neededSmsToFinalize;
    }

    public void setChipNumber(String chipNumber) {
        this.chipNumber = chipNumber;
    }

    public void setNeededSmsToFinalize(Integer neededSmsToFinalize) {
        this.neededSmsToFinalize = neededSmsToFinalize;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public ActivationStatusEnum getStatusDesc() {
        return ActivationStatusEnum.fromId(this.status);
    }

    public LocalDateTime getInitialTime() {
        return this.initialTime;
    }

    public void setInitialTime(LocalDateTime initialTime) {
        this.initialTime = initialTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setVisible(int i) {
        this.visible = i;
    }

    public void setReceivedToFinishTime(String receivedToFinishTime) {
        this.receivedToFinishTime = receivedToFinishTime;
    }
}
