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
 *  org.hibernate.annotations.Fetch
 *  org.hibernate.annotations.FetchMode
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
public class ChipNumberControl
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String chipNumber;
    @ElementCollection
    @CollectionTable(name="chip_number_control_alias_service", joinColumns={@JoinColumn(name="chip_number_control_id")})
    @Column(name="alias_service")
    @Fetch(value=FetchMode.JOIN)
    private List<String> aliasService = new ArrayList<String>();

    public ChipNumberControl() {
    }

    public ChipNumberControl(String chipNumber) {
        this.chipNumber = chipNumber;
    }

    public Long getId() {
        return this.id;
    }

    public String getChipNumber() {
        return this.chipNumber;
    }

    public void setChipNumber(String chipNumber) {
        this.chipNumber = chipNumber;
    }

    public List<String> getAliasService() {
        return this.aliasService;
    }

    public void setAliasService(List<String> aliasService) {
        this.aliasService = aliasService;
    }
}
