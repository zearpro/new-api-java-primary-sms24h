/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.VersionEnum;
import br.com.store24h.store24h.model.TimeZone;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="registro_de_compras")
public class CompraServiso
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long idActivation;
    private LocalDateTime localDateTime;
    private String aliasService;
    private String number;
    private String sms = "";
    private BigDecimal cost;
    private int status = 1;
    private Long idUser;
    private int visible;
    private int version;

    public CompraServiso() {
    }

    public CompraServiso(Long idActivation, String aliasService, String number, BigDecimal cost, Long idUser) {
        this.idActivation = idActivation;
        this.localDateTime = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone()));
        this.aliasService = aliasService;
        this.number = number;
        this.cost = cost;
        this.idUser = idUser;
        this.version = Utils.getVersionNumber();
        this.visible = Utils.getVersion().equals((Object)VersionEnum.VERSION_2) ? 0 : 1;
    }

    public CompraServiso(Long idActivation, String aliasService, String number, BigDecimal cost, Long idUser, int version, int visible) {
        this.idActivation = idActivation;
        this.localDateTime = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone()));
        this.aliasService = aliasService;
        this.number = number;
        this.cost = cost;
        this.idUser = idUser;
        this.version = version;
        this.visible = visible;
    }

    public Long getId() {
        return this.id;
    }

    public String getAliasService() {
        return this.aliasService;
    }

    public void setAliasService(String aliasService) {
        this.aliasService = aliasService;
    }

    public LocalDateTime getLocalDateTime() {
        return this.localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getIdActivation() {
        return this.idActivation;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
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

    public void setVisible(int i) {
        this.visible = 1;
    }
}
