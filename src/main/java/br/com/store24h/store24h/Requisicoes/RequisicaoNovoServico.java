/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.Requisicoes;

import br.com.store24h.store24h.model.Servico;
import java.math.BigDecimal;
import java.util.Map;

public class RequisicaoNovoServico {
    private String SENHA;
    private String name;
    private String alias;
    private BigDecimal price;
    private BigDecimal defaultPrice;
    private boolean defaultMaxPrice;
    private BigDecimal maxPrice;
    private Map<String, Integer> priceMap;

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

    public BigDecimal getMaxPrice() {
        return this.maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Map<String, Integer> getPriceMap() {
        return this.priceMap;
    }

    public void setPriceMap(Map<String, Integer> priceMap) {
        this.priceMap = priceMap;
    }

    public Servico toServico() {
        Servico serv = new Servico();
        serv.setName(this.name);
        serv.setAlias(this.alias);
        serv.setPrice(this.price);
        serv.setDefaultPrice(this.defaultPrice);
        serv.setDefaultMaxPrice(this.defaultMaxPrice);
        serv.setMaxPrice(this.maxPrice);
        return serv;
    }
}
