/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Condition
 *  org.springframework.context.annotation.ConditionContext
 *  org.springframework.core.type.AnnotatedTypeMetadata
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.ConditionalScheduling;
import br.com.store24h.store24h.Funcionalidades.CronCheck;
import java.util.Map;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RunSchedulingCondition
implements Condition {
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map attributes = metadata.getAnnotationAttributes(ConditionalScheduling.class.getName());
        if (attributes == null) {
            return false;
        }
        String scheduleName = (String)attributes.get("scheduleName");
        boolean canrun = CronCheck.canRunCron(scheduleName);
        return canrun;
    }
}
