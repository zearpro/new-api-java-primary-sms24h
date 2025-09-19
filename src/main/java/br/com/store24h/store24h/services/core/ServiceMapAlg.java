/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 */
package br.com.store24h.store24h.services.core;

import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapAlg {
    private final HashMap<String, String> abrev = new HashMap<String, String>(100){
        {
            this.put("wa", "Codigo do WhatsApp([ Business:]? \\d{3,6}-\\d{3,6})|WhatsApp code([ Business:]? \\d{3,6}-\\d{3,6})");
            this.put("ka", "Shopee: crie sua conta usando o codigo de verificacao (\\d{4,6})");
            this.put("ki", "<99 >Seu codigo de verificacao e \\((\\d{4,6})");
            this.put("sn", "Use (\\d{4,6}) para validar seu telefone na OLX");
            this.put("tn", "Seu codigo de verificacao do LinkedIn e (\\d{4,6})");
            this.put("ub", "Seu codigo Uber \ufffd (\\d{4,6})");
            this.put("vp", "\ufffdKwai\ufffd4248 is your verification code(\\d{4,6})");
            this.put("am", "(\\d{4,6}) e seu codigo de verificacao da Amazon");
            this.put("dh", "eBay: o seu codigo de seguranca e (\\d{4,6})");
        }
    };
    private final HashMap<String, String> abrevReverse = new HashMap<String, String>(100){
        {
            this.put("01", "dh");
            this.put("02", "eh");
            this.put("03", "fb");
            this.put("04", "ig");
            this.put("05", "ka");
            this.put("06", "ki");
            this.put("07", "lj");
            this.put("08", "mm");
            this.put("09", "mt");
            this.put("a1", "nf");
            this.put("a2", "sn");
            this.put("29222", "tg");
            this.put("a3", "tn");
            this.put("a4", "ts");
            this.put("a5", "tw");
            this.put("a6", "ub");
            this.put("a7", "uk");
            this.put("a8", "vp");
            this.put("wa", "WhatsApp code (\\d{3,6}-\\d{3,6})");
            this.put("??", "29468");
            this.put("a9", "wx");
        }
    };

    public String getRegxByAlias(String alias) {
        return this.abrev.get(alias);
    }

    public String getAliasByNumber(String serviceNumber) {
        return this.abrevReverse.get(serviceNumber);
    }
}
