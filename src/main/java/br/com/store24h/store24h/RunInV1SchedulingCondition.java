/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Condition
 *  org.springframework.context.annotation.ConditionContext
 *  org.springframework.core.type.AnnotatedTypeMetadata
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.VersionEnum;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RunInV1SchedulingCondition
implements Condition {
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !Utils.getVersion().equals((Object)VersionEnum.VERSION_2);
    }
}
