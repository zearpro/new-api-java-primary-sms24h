/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h.services.core;

import java.util.HashMap;

class ServiceMapAlg$1
extends HashMap<String, String> {
    ServiceMapAlg$1(int initialCapacity) {
        super(initialCapacity);
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
}
