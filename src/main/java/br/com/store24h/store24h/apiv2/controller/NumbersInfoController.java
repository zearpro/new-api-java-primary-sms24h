/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.media.Content
 *  io.swagger.v3.oas.annotations.media.ExampleObject
 *  io.swagger.v3.oas.annotations.responses.ApiResponse
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Conditional
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.apiv2.controller;

import br.com.store24h.store24h.RunInV2SchedulingCondition;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.BuyNumberErrorResponse;
import br.com.store24h.store24h.apiv2.NumbersDisponibleDTO;
import br.com.store24h.store24h.apiv2.NumbersPriceDTO;
import br.com.store24h.store24h.apiv2.ServicesListDTO;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.model.Operadoras;
import br.com.store24h.store24h.repository.OperadorasRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Numbers stats")
@RestController
@Conditional(value={RunInV2SchedulingCondition.class})
@RequestMapping(value={"/api/v1/info/"})
public class NumbersInfoController {
    @Autowired
    OperadorasRepository operadorasRepository;
    @Autowired
    ServicosRepository servicosRepository;
    @Autowired
    private CacheService cacheService;

    @GetMapping(value={"/services"})
    @Operation(summary="Service list")
    @ApiResponse(responseCode="200", description="Services list", content={@Content(mediaType="application/json", examples={@ExampleObject(value="[\n   {\n    \"name\": \"appName\",\n    \"code\": \"xx\",\n  },\n]")})})
    public ResponseEntity servicesList(@RequestParam(value="apiKey") String apiKey) {
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        List servicos = this.servicosRepository.findAll().stream().map(servico -> new ServicesListDTO(servico.getName(), servico.getAlias())).collect(Collectors.toList());
        return ResponseEntity.ok(servicos);
    }

    @GetMapping(value={"/operators"})
    @Operation(summary="Operators list")
    @ApiResponse(responseCode="200", description="Operators list", content={@Content(mediaType="application/json", examples={@ExampleObject(value="[\n  \"ARQIA\",\n  \"CLARO\",\n  \"CORREIOS_CELULAR\",\n  \"VIVO\"\n]")})})
    public ResponseEntity operators(@RequestParam(value="apiKey") String apiKey) {
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        List operadoras = this.operadorasRepository.findAll().stream().map(Operadoras::getOperadora).collect(Collectors.toList());
        return ResponseEntity.ok(operadoras);
    }

    @GetMapping(value={"/disponible"})
    @Operation(summary="Quantity disponible for every service")
    @ApiResponse(responseCode="200", description="Quantity disponible for every service", content={@Content(mediaType="application/json", examples={@ExampleObject(value="[\n    {\n      \"name\": \"appName\",\n      \"code\": \"xx\",\n      \"quantity\": 132306\n    }\n]")})})
    public ResponseEntity disponible(@RequestParam(value="apiKey") String apiKey, Optional<String> operator, Optional<String> country) {
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        List servicos = !country.isPresent() ? this.servicosRepository.findAll().stream().map(servico -> new NumbersDisponibleDTO(servico.getName(), servico.getAlias(), 0)).collect(Collectors.toList()) : this.servicosRepository.findAll().stream().map(servico -> new NumbersDisponibleDTO(servico.getName(), servico.getAlias(), servico.getTotalQuantity(operator))).collect(Collectors.toList());
        return ResponseEntity.ok(servicos);
    }

    @GetMapping(value={"/prices"})
    @Operation(summary="Services price list")
    @ApiResponse(responseCode="200", description="Service prices", content={@Content(mediaType="application/json", examples={@ExampleObject(value="[\n   {\n    \"name\": \"appName\",\n    \"code\": \"xx\",\n    \"price\": 0.1\n  },\n]")})})
    public ResponseEntity prices(@RequestParam(value="apiKey") String apiKey) {
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        List servicos = this.servicosRepository.findAll().stream().map(servico -> new NumbersPriceDTO(servico.getName(), servico.getAlias(), servico.getPrice())).collect(Collectors.toList());
        return ResponseEntity.ok(servicos);
    }
}
