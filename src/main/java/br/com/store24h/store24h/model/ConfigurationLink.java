/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Entity
 *  javax.persistence.GeneratedValue
 *  javax.persistence.GenerationType
 *  javax.persistence.Id
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ConfigurationLink
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String apiLink;

    public ConfigurationLink() {
    }

    public ConfigurationLink(String apiLink) {
        this.apiLink = apiLink;
    }

    public String getApiLink() {
        return this.apiLink;
    }

    public void setApiLink(String apiLink) {
        this.apiLink = apiLink;
    }
}
