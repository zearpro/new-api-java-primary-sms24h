/*
 * Decompiled with CFR 0.152.
 */
package br.com.store24h.store24h;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalPercentages {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public static BigDecimal toPercentageOf(BigDecimal value, BigDecimal total) {
        return value.divide(total, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
    }

    public static BigDecimal percentOf(BigDecimal percentage, BigDecimal total) {
        return percentage.multiply(total).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
