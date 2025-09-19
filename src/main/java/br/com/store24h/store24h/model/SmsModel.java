/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="sms_model", indexes={@Index(name="idx_emissor", columnList="emissor"), @Index(name="idx_chipnumber", columnList="chipnumber")})
public class SmsModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    @Column(unique=true, nullable=false)
    private Long idActivation;
    private String msg;
    @Column(nullable=false)
    private String emissor;
    @Column(unique=true, nullable=false)
    private LocalDateTime date;
    @Column(nullable=false)
    private Integer sequencia;
    @Column(nullable=true)
    private Integer pretry;
    @Column(nullable=false)
    private String chipnumber;
    @Column(nullable=true)
    private String msg_full;

    public SmsModel(String msg, String emissor, LocalDateTime date, Integer sequencia, String msg_full) {
        this.msg = msg;
        this.emissor = emissor;
        this.date = date;
        this.sequencia = sequencia;
        this.msg_full = msg_full;
    }

    public SmsModel() {
    }

    public Long getIdActivation() {
        return this.idActivation;
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public String getChipnumber() {
        return this.chipnumber;
    }

    public void setChipnumber(String chipnumber) {
        this.chipnumber = chipnumber;
    }

    public long getId() {
        return this.id;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getEmissor() {
        return this.emissor;
    }

    public void setEmissor(String emissor) {
        this.emissor = emissor;
    }

    public LocalDateTime getDate() {
        return this.date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getSequencia() {
        return this.sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getMsg_full() {
        if (this.msg_full == null) {
            return "";
        }
        return this.msg_full;
    }

    public Integer getPretry() {
        return this.pretry;
    }

    public void setPretry(Integer pretry) {
        this.pretry = pretry;
    }

    public void setMsg_full(String msg_full) {
        this.msg_full = msg_full;
    }

    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.msg);
        hash = 83 * hash + Objects.hashCode(this.emissor);
        hash = 83 * hash + Objects.hashCode(this.date);
        return hash;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        SmsModel other = (SmsModel)obj;
        if (!Objects.equals(this.msg, other.msg)) {
            return false;
        }
        if (!Objects.equals(this.emissor, other.emissor)) {
            return false;
        }
        return Objects.equals(this.date, other.date);
    }
}
