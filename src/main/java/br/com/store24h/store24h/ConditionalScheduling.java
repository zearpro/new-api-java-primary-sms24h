/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Conditional
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.RunSchedulingCondition;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

@Target(value={ElementType.TYPE, ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Conditional(value={RunSchedulingCondition.class})
public @interface ConditionalScheduling {
    public String scheduleName();
}
