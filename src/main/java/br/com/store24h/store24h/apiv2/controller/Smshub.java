/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  org.springframework.context.annotation.Conditional
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.apiv2.controller;

import br.com.store24h.store24h.RunInV2SchedulingCondition;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Conditional(value={RunInV2SchedulingCondition.class})
@RequestMapping(value={"/smshub"})
public class Smshub {
    @PostMapping
    @Operation(summary="1. API used by smshub.org")
    public ResponseEntity disabled() {
        return ResponseEntity.ok((Object)"DOMAIN ENABLED: api.sms24h.com -> access via api.sms24h.com/smshub/");
    }
}
