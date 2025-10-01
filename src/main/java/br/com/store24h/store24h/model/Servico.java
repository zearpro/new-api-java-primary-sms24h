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
 *  javax.validation.constraints.PositiveOrZero
 *  org.json.JSONObject
 */
package br.com.store24h.store24h.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.PositiveOrZero;
import org.json.JSONObject;

@Entity
@Table(name="servicos", indexes={@Index(name="idx_alias", columnList="alias")})
public class Servico
implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String name;
    @Column(unique=true, nullable=false)
    private String alias;
    @Column(nullable=false)
    private BigDecimal price;
    @Column(nullable=false)
    private BigDecimal defaultPrice;
    @Column(nullable=false)
    private boolean defaultMaxPrice;
    @Column(nullable=false)
    private BigDecimal maxPrice;
    private boolean random;
    private int quantityForMaxPrice = 30;
    private boolean activity = true;
    @PositiveOrZero
    private int totalQuantity;
    private boolean canAuction;
    private ArrayList<Object> auctionMap = new ArrayList();
    private boolean work;
    @Column(name="by_operator", nullable=true)
    private String byOperator;
    @Column(name="smshub_habilitado", nullable=false, columnDefinition="INTEGER DEFAULT 0")
    private Integer smshub = 1;

    public Servico() {
    }

    public Servico(String name, String alias, BigDecimal price) {
        this.name = name;
        this.alias = alias;
        this.price = price;
        this.defaultPrice = price;
        this.maxPrice = price;
        this.defaultMaxPrice = true;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMaxPrice() {
        return this.maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public BigDecimal getDefaultPrice() {
        return this.defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public boolean isDefaultMaxPrice() {
        return this.defaultMaxPrice;
    }

    public void setDefaultMaxPrice(boolean defaultMaxPrice) {
        this.defaultMaxPrice = defaultMaxPrice;
    }

    public boolean isRandom() {
        return this.random;
    }

    public boolean isActivity() {
        return this.activity;
    }

    public void setActivity(boolean activity) {
        this.activity = activity;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public int getQuantityForMaxPrice() {
        return this.quantityForMaxPrice;
    }

    public void setQuantityForMaxPrice(int quantityForMaxPrice) {
        this.quantityForMaxPrice = quantityForMaxPrice;
    }

    public int getTotalQuantity() {
        return this.totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public boolean isCanAuction() {
        return this.canAuction;
    }

    public void setCanAuction(boolean canAuction) {
        this.canAuction = canAuction;
    }

    public ArrayList<Object> getAuctionMap() {
        return this.auctionMap;
    }

    public void setAuctionMap(ArrayList<Object> auctionMap) {
        this.auctionMap = auctionMap;
    }

    public boolean isWork() {
        return this.work;
    }

    public void setWork(boolean work) {
        this.work = work;
    }

    public Integer isSmshub() {
        return this.smshub;
    }

    public void setSmshub(int enb) {
        this.smshub = enb;
    }

    public String getByOperator() {
        return this.byOperator;
    }

    public int getTotalQuantity(Optional<String> operator) {
        if (operator.isPresent()) {
            try {
                JSONObject obj = new JSONObject(this.byOperator);
                JSONObject operadora = obj.getJSONObject(operator.get().toLowerCase());
                return operadora.optInt("disponible", 0);
            }
            catch (Exception e) {
                return 0;
            }
        }
        return this.totalQuantity;
    }
}
