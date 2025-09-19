/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.FetchType
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 *  javax.persistence.JoinColumn
 *  javax.persistence.ManyToOne
 *  javax.persistence.Table
 */
package br.com.store24h.store24h.model;

import br.com.store24h.store24h.model.Activation;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="sms_string_models")
public class SmsStringModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="activation_id", nullable=false)
    private Activation activation;
    @Column(name="sms_string_models", length=255)
    private String smsStringModel;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activation getActivation() {
        return this.activation;
    }

    public void setActivation(Activation activation) {
        this.activation = activation;
    }

    public String getSmsStringModel() {
        return this.smsStringModel;
    }

    public void setSmsStringModel(String smsStringModel) {
        this.smsStringModel = smsStringModel;
    }
}
