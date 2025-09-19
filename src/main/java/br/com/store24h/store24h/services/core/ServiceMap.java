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
public class ServiceMap {
    private final HashMap<String, String> abrev = new HashMap<String, String>(100){
        {
            this.put("wx", "0");
            this.put("eh", "0");
            this.put("lj", "0");
            this.put("nf", "0");
            this.put("ts", "0");
            this.put("fb", "27100");
            this.put("ka", "28149");
            this.put("ki", "27100");
            this.put("sn", "28908");
            this.put("tg", "29222");
            this.put("tn", "29454");
            this.put("tw", "5655545156");
            this.put("ub", "5655545156");
            this.put("mm", "29795");
            this.put("vp", "29415");
            this.put("uk", "29090");
            this.put("wa", "29468");
            this.put("??", "29468");
            this.put("ig", "29468");
            this.put("kx", "103");
            this.put("pd", "7370797968");
            this.put("am", "28060");
            this.put("dh", "28060");
            this.put("mt", "28060");
            this.put("ds", "65858472778371");
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
            this.put("wa", "29468");
            this.put("??", "29468");
            this.put("a9", "wx");
        }
    };

    public String getNumberByAlias(String alias) {
        return this.abrev.get(alias);
    }

    public String getAliasByNumber(String serviceNumber) {
        return this.abrevReverse.get(serviceNumber);
    }
}
