/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.media.Content
 *  io.swagger.v3.oas.annotations.media.ExampleObject
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.responses.ApiResponse
 *  io.swagger.v3.oas.annotations.responses.ApiResponses
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Conditional
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.apiv2.controller;

import br.com.store24h.store24h.RunInV2SchedulingCondition;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.ActionsPermitedEnum;
import br.com.store24h.store24h.apiv2.BuyNumberDto;
import br.com.store24h.store24h.apiv2.BuyNumberErrorResponse;
import br.com.store24h.store24h.apiv2.BuyNumberResponse;
import br.com.store24h.store24h.apiv2.CodeResponseStatusEnum;
import br.com.store24h.store24h.apiv2.GetCodeResponse;
import br.com.store24h.store24h.apiv2.ReBuyNumberDto;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.exceptions.ApiKeyNotFoundException;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.ActivationStatusEnum;
import br.com.store24h.store24h.services.core.ActivationStatus;
import br.com.store24h.store24h.services.core.PublicApiService;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Conditional(value={RunInV2SchedulingCondition.class})
@Tag(name="Buy Code for Service")
@RequestMapping(value={"/api/v1/buy/"})
public class BuyNumbersController {
    @Autowired
    private PublicApiService methodsHubService;
    @Autowired
    private CacheService cacheService;

    @PostMapping
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Successfully retrieved service activation", content={@Content(mediaType="application/json", schema=@Schema(implementation=BuyNumberResponse.class))}), @ApiResponse(responseCode="400", description="User without balance", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"NO_BALANCE\"\n}")})}), @ApiResponse(responseCode="   400", description="Service blocked for this user", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"FORBIDEN_SERVICE\"\n}")})}), @ApiResponse(responseCode="    400", description="Service not found", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"BAD_SERVICE\"\n}")})}), @ApiResponse(responseCode="     400", description="No numbers disponible", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"NO_NUMBERS\"\n}")})}), @ApiResponse(responseCode="      400", description="Service or Country not found", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"BAD_ACTION\"\n}")})}), @ApiResponse(responseCode="   401", description="API Key Error", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n  \"status\": \"failed\",\n  \"error\": \"API_KEY_NOT_FOUND\"\n}")})})})
    @Operation(summary="1. Buy Service Number")
    public ResponseEntity buyService(@RequestBody BuyNumberDto buyNumberRequest) {
        try {
            Utils.checkUserApiType(this.cacheService, buyNumberRequest.getApiKey(), TipoDeApiEnum.SISTEMAS);
            String resp = this.methodsHubService.getNumber(buyNumberRequest.getApiKey(), Optional.of(buyNumberRequest.getService()), Optional.of(buyNumberRequest.getOperator()), Optional.of(buyNumberRequest.getCountry()), Optional.empty(), 2, TipoDeApiEnum.SISTEMAS);
            if (resp.contains("ACCESS_NUMBER")) {
                String[] parts = resp.split(":");
                String id = parts[1];
                String number = parts[2];
                return new ResponseEntity((Object)new BuyNumberResponse(id, number, buyNumberRequest.getService()), HttpStatus.CREATED);
            }
            String[] parts = resp.split(":");
            String errorMessage = parts[0];
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", errorMessage), HttpStatus.BAD_REQUEST);
        }
        catch (ApiKeyNotFoundException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "API_KEY_NOT_FOUND"), HttpStatus.UNAUTHORIZED);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value={"/extra/buy"})
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Successfully retrieved service activation", content={@Content(mediaType="application/json", schema=@Schema(implementation=BuyNumberResponse.class))}), @ApiResponse(responseCode="400", description="User without balance", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"NO_BALANCE\"\n}")})}), @ApiResponse(responseCode="   400", description="Service blocked for this user", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"FORBIDEN_SERVICE\"\n}")})}), @ApiResponse(responseCode="    400", description="Service not found", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"BAD_SERVICE\"\n}")})}), @ApiResponse(responseCode="     400", description="No numbers disponible", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"NO_NUMBERS\"\n}")})}), @ApiResponse(responseCode="      400", description="Service or Country not found", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n    \"status\": \"failed\",\n    \"error\": \"BAD_ACTION\"\n}")})}), @ApiResponse(responseCode="   401", description="API Key Error", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n  \"status\": \"failed\",\n  \"error\": \"API_KEY_NOT_FOUND\"\n}")})})})
    @Operation(summary="2. Re-buy Service Number")
    public ResponseEntity reBuyService(@RequestBody ReBuyNumberDto buyNumberRequest) {
        try {
            Utils.checkUserApiType(this.cacheService, buyNumberRequest.getApiKey(), TipoDeApiEnum.SISTEMAS);
            Activation activationAnterior = this.methodsHubService.getActivation(Long.valueOf(buyNumberRequest.getActivation_id()));
            if (activationAnterior == null || !activationAnterior.getStatusBuz().equals((Object)ActivationStatus.FINALIZADA) || !activationAnterior.getApiKey().equals(buyNumberRequest.getApiKey())) {
                return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "BAD_ACTION"), HttpStatus.BAD_REQUEST);
            }
            Optional<String> service = Optional.of(activationAnterior.getAliasService().split("_")[0]);
            Optional<String> numero = Optional.of(activationAnterior.getChipNumber());
            String resp = this.methodsHubService.getNumber(buyNumberRequest.getApiKey(), service, Optional.of("ANY"), Optional.of(buyNumberRequest.getCountry()), numero, 2, TipoDeApiEnum.SISTEMAS);
            if (resp.contains("ACCESS_NUMBER")) {
                String[] parts = resp.split(":");
                String id = parts[1];
                String number = parts[2];
                return new ResponseEntity((Object)new BuyNumberResponse(id, number, service.get()), HttpStatus.CREATED);
            }
            String[] parts = resp.split(":");
            String errorMessage = parts[0];
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", errorMessage), HttpStatus.BAD_REQUEST);
        }
        catch (ApiKeyNotFoundException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "API_KEY_NOT_FOUND"), HttpStatus.UNAUTHORIZED);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(value={"/set-status"})
    @ApiResponses(value={@ApiResponse(responseCode="200", description="1. Successfully set status of activation", content={@Content(mediaType="application/json", schema=@Schema(implementation=GetCodeResponse.class, example="{\"error\":null,\"status\":\"FINISHED\",\"code\":\"Seu codigo e 318-882\",\"number\":\"5521988776655\",\"service\":\"ot\",\"actionsPermited\":[]}"))}), @ApiResponse(responseCode="400", description="2. Action not permited", content={@Content(mediaType="application/json", schema=@Schema(example="{\"error\":\"ACTION_STATUS_NOT_PERMITED\",\"status\":\"FINISHED\",\"code\":\"Seu codigo e 880-792\",\"number\":\"5521988776655\",\"service\":\"ot\",\"actionsPermited\":[]}"))}), @ApiResponse(responseCode="403      ", description="3. Activation not found or API Key Invalid", content={@Content(mediaType="application/json", schema=@Schema(example="{\"error\":\"NO_ACTIVATION\",\"status\":null,\"code\":null,\"number\":null,\"service\":null,\"actionsPermited\":[]}"))})})
    @Operation(summary="4. Set Activation Status")
    public ResponseEntity setStatus(@RequestParam(value="id") long id, @RequestParam(value="apiKey") String apiKey, @RequestParam(value="status") ActionsPermitedEnum status) {
        boolean hasError;
        if (id < 0L || apiKey == null || status == null) {
            return new ResponseEntity((Object)new GetCodeResponse(id, "", "", null, CodeResponseStatusEnum.BAD_ACTION, ""), HttpStatus.BAD_REQUEST);
        }
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        GetCodeResponse responseGetStatus = this.methodsHubService.getActivationStatus(Optional.of(id), apiKey);
        boolean bl = hasError = responseGetStatus.getError() != null;
        if (responseGetStatus.getActionsPermited().contains((Object)status)) {
            String response = "BAD_ACTION";
            ActivationStatusEnum newStatus = responseGetStatus.getStatusEnum();
            if (status.equals((Object)ActionsPermitedEnum.CANCEL)) {
                response = this.methodsHubService.setStatus(Optional.of(8), Optional.of(id), apiKey);
            } else if (status.equals((Object)ActionsPermitedEnum.FINISH)) {
                response = this.methodsHubService.setStatus(Optional.of(6), Optional.of(id), apiKey);
            } else if (status.equals((Object)ActionsPermitedEnum.RETRY)) {
                response = this.methodsHubService.setStatus(Optional.of(3), Optional.of(id), apiKey);
            }
            CodeResponseStatusEnum statusResponse = CodeResponseStatusEnum.valueOf(response);
            if (statusResponse.equals((Object)CodeResponseStatusEnum.ACCESS_ACTIVATION)) {
                newStatus = ActivationStatusEnum.FINISHED;
            } else if (statusResponse.equals((Object)CodeResponseStatusEnum.ACCESS_RETRY_GET)) {
                newStatus = ActivationStatusEnum.STATUS_WAIT_RETRY;
            } else if (statusResponse.equals((Object)CodeResponseStatusEnum.ACCESS_CANCEL) || statusResponse.equals((Object)CodeResponseStatusEnum.ACCESS_CANCEL_DUPLICATED)) {
                newStatus = ActivationStatusEnum.STATUS_CANCEL;
            }
            responseGetStatus.setStatus(newStatus, ActivationStatusEnum.toId(newStatus));
            return new ResponseEntity((Object)responseGetStatus, HttpStatus.OK);
        }
        if (hasError && responseGetStatus.getError().equals((Object)CodeResponseStatusEnum.NO_ACTIVATION)) {
            return new ResponseEntity((Object)responseGetStatus, HttpStatus.FORBIDDEN);
        }
        responseGetStatus.setError(CodeResponseStatusEnum.ACTION_STATUS_NOT_PERMITED);
        return new ResponseEntity((Object)responseGetStatus, HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value={"/get-code"})
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Activation waiting receive sms", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n  \"error\": null,\n  \"status\": \"STATUS_WAIT_CODE\",\n  \"code\": null,\n  \"number\": \"5521988776655\",\n  \"service\": \"ot\",\n  \"actionsPermited\": [\n    \"CANCEL\"\n  ]\n}\n")})}), @ApiResponse(responseCode=" 200 ", description="Activation received SMS", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n  \"error\": null,\n  \"status\": \"STATUS_OK\",\n  \"code\": \"Seu codigo e 678-636\",\n  \"number\": \"5521920400955\",\n  \"service\": \"ot\",\n  \"actionsPermited\": [\n    \"FINISH\",\n    \"RETRY\"\n  ]\n}")})}), @ApiResponse(responseCode="   200  ", description="Activation waiting receive RETRY SMS or FINISH", content={@Content(mediaType="application/json", examples={@ExampleObject(value="{\n  \"error\": null,\n  \"status\": \"STATUS_WAIT_RETRY\",\n  \"code\": \"Seu codigo e 678-636\",\n  \"number\": \"5521920400955\",\n  \"service\": \"ot\",\n  \"actionsPermited\": [\n    \"FINISH\"\n  ]\n}")})}), @ApiResponse(responseCode="     403       ", description="4. Activation not found or API Key Invalid", content={@Content(mediaType="application/json", schema=@Schema(example="{\"error\":\"NO_ACTIVATION\",\"status\":null,\"code\":null,\"number\":null,\"service\":null,\"actionsPermited\":[]}"))})})
    @Operation(summary="3. Get Service Code and Status")
    public ResponseEntity getCode(@RequestParam(value="id") long id, @RequestParam(value="apiKey") String apiKey) {
        if (apiKey == null) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        try {
            Utils.checkUserApiType(this.cacheService, apiKey, TipoDeApiEnum.SISTEMAS);
        }
        catch (TipoDeApiNotPermitedException e) {
            return new ResponseEntity((Object)new BuyNumberErrorResponse("failed", "VERSION_NOT_PERMITED"), HttpStatus.UNAUTHORIZED);
        }
        GetCodeResponse responseGetStatus = this.methodsHubService.getActivationStatus(Optional.of(id), apiKey);
        if (responseGetStatus.getError() != null) {
            return new ResponseEntity((Object)responseGetStatus, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity((Object)responseGetStatus, HttpStatus.OK);
    }
}
