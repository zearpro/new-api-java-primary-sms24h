/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.nimbusds.jose.shaded.json.JSONObject
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  javax.servlet.http.HttpServletRequest
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.exceptions.ApiKeyNotFoundException;
import br.com.store24h.store24h.apiv2.services.NumerosService;
import br.com.store24h.store24h.dto.SmsDTO;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.repository.ActivationRepository;
import br.com.store24h.store24h.repository.AdmDbRepository;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.services.OtherService;
import br.com.store24h.store24h.services.SvsService;
import br.com.store24h.store24h.services.UserService;
import br.com.store24h.store24h.services.core.ActivationService;
import br.com.store24h.store24h.services.core.ActivationStatus;
import br.com.store24h.store24h.services.core.PublicApiService;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import org.json.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Handler api", description="Handler api")
@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class SmsApi {
    Logger logger = LoggerFactory.getLogger(SmsApi.class);
    @Autowired
    private AdmDbRepository admDbRepository;
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private ServicosRepository servicosRepository;
    @Autowired
    private ActivationRepository activationRepository;
    @Autowired
    private ChipRepository chipRepository;
    @Autowired
    private SvsService svsService;
    @Autowired
    private ChipNumberControlService controlService;
    @Autowired
    private Funcionalidades funcionalidades;
    @Autowired
    private UserService userService;
    @Autowired
    private PublicApiService methodsHubService;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private OtherService outherService;
    @Autowired
    NumerosService numerosService;
    private static final String REQUEST_ID = "requestId";

    @GetMapping
    public synchronized ResponseEntity<Object> entryPoint(HttpServletRequest request, @RequestParam(value="api_key") String apiKey, @RequestParam(value="action") String action, Optional<String> country, Optional<String> operator, Optional<String> service, Optional<Long> id, Optional<Integer> status, Optional<String> reqID, Optional<String> activationId, Optional<String> showbalance, Optional<String> smshubEnabled) throws NoSuchMethodException, ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        String requestId = "request-id-desconhecido";
        try {
            requestId = (String)request.getAttribute(REQUEST_ID);
        }
        catch (Exception exception) {
            // empty catch block
        }
        String responseAPI = "";
        if (System.getenv("IS_SMSHUB") == null && !this.userService.isValidApiKey(apiKey)) {
            responseAPI = "BAD_KEY";
            return ResponseEntity.badRequest().body((Object)responseAPI);
        }
        if (action.equals("getBalance")) {
            String responseGetBalancer = this.methodsHubService.getBalancer(apiKey);
            if (responseGetBalancer.equals("BAD_ACTION")) {
                return ResponseEntity.badRequest().body((Object)responseGetBalancer);
            }
            return ResponseEntity.ok((Object)responseGetBalancer);
        }
        if (action.equals("getNumber")) {
            List<List<String>> activationList;
            long startTime = System.nanoTime();
            if (country.isPresent() && !country.get().equals("73")) {
                return ResponseEntity.badRequest().body((Object)"BAD_SERVICE");
            }
            if (System.getenv("IS_SMSHUB") == null && (activationList = this.numerosService.getActivationsValids(apiKey)).size() >= 100 && !apiKey.equals("af283f11baf99de7ad739eef512cbef8") && !apiKey.equals("2bce230ccb852582f693e803def487aa")) {
                return ResponseEntity.badRequest().body((Object)"LIMITED_ACTIVATIONS");
            }
            Object responseGetNumber = "";
            try {
                responseGetNumber = this.methodsHubService.getNumber(apiKey, service, operator, country);
            }
            catch (ApiKeyNotFoundException ex) {
                responseAPI = "BAD_KEY";
                return ResponseEntity.badRequest().body((Object)responseAPI);
            }
            if (showbalance.isPresent()) {
                responseGetNumber = (String)responseGetNumber + ":balance:" + this.methodsHubService.getBalancer(apiKey);
            }
            List<String> badResponse = Arrays.asList("BAD_ACTION", "BAD_SERVICE", "ERROR_SQL");
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            double durationSeconds = (double)durationNanos / 1.0E9;
            this.logger.info("[{} s]TEMPO GASTO PARA GET NUMBER: {}", (Object)durationSeconds, responseGetNumber);
            if (badResponse.contains(responseGetNumber)) {
                return ResponseEntity.badRequest().body(responseGetNumber);
            }
            return ResponseEntity.ok((Object)responseGetNumber);
        }
        if (action.equals("getExtraActivation")) {
            Activation activationAnterior = this.methodsHubService.getActivation(Long.valueOf(activationId.get()));
            if (activationAnterior == null || !activationAnterior.getStatusBuz().equals((Object)ActivationStatus.FINALIZADA) || !activationAnterior.getApiKey().equals(apiKey)) {
                // empty if block
            }
            ArrayList<ActivationStatus> statusAguardando = new ArrayList<ActivationStatus>();
            statusAguardando.add(ActivationStatus.AGUARDANDO_MENSAGENS);
            statusAguardando.add(ActivationStatus.SOLICITADA);
            String numero = activationAnterior.getChipNumber();
            List<Activation> ativacoesParaEsteNumeroAguardando = this.activationRepository.findByStatusBuzInAndChipNumberAndAliasService(statusAguardando, numero, activationAnterior.getAliasService().split("_")[0]);
            if (ativacoesParaEsteNumeroAguardando.size() > 0) {
                return ResponseEntity.badRequest().body((Object)"RENEW_ACTIVATION_NOT_AVAILABLE");
            }
            Optional<String> empty = Optional.empty();
            Optional<String> optionalNumeroLista = Optional.ofNullable(numero);
            Optional<String> optionalCountry = country;
            String responseGetNumber = "";
            try {
                responseGetNumber = this.methodsHubService.getNumber(apiKey, Optional.of(activationAnterior.getAliasService().split("_")[0]), empty, optionalCountry, optionalNumeroLista, 1, TipoDeApiEnum.ANTIGA);
            }
            catch (ApiKeyNotFoundException ex) {
                responseAPI = "BAD_KEY";
                return ResponseEntity.badRequest().body((Object)responseAPI);
            }
            List<String> badResponse = Arrays.asList("RENEW_ACTIVATION_NOT_AVAILABLE", "BAD_ACTION", "BAD_SERVICE", "ERROR_SQL");
            if (responseGetNumber.contains("NO_NUMBERS")) {
                responseGetNumber = "RENEW_ACTIVATION_NOT_AVAILABLE";
            }
            if (badResponse.contains(responseGetNumber)) {
                return ResponseEntity.badRequest().body((Object)responseGetNumber);
            }
            request.setAttribute("activationId", (Object)responseGetNumber);
            return ResponseEntity.ok((Object)responseGetNumber);
        }
        if (action.equals("getStatus")) {
            String responseGetStatus = this.methodsHubService.getStatus(id, apiKey);
            List<String> badResponse = Arrays.asList("BAD_ACTION", "NO_ACTIVATION", "ERROR_SQL");
            if (badResponse.contains(responseGetStatus)) {
                return ResponseEntity.badRequest().body((Object)responseGetStatus);
            }
            return ResponseEntity.ok((Object)responseGetStatus);
        }
        if (action.equals("getSms")) {
            SmsDTO smsDTO = this.methodsHubService.getSms(id.get());
            return ResponseEntity.ok((Object)smsDTO);
        }
        if (action.equals("getSmsRetry")) {
            SmsDTO smsDTO = this.methodsHubService.getSmsRetry(id.get());
            return ResponseEntity.ok((Object)smsDTO);
        }
        if (action.equals("setStatus")) {
            Object responseSetStatus = this.methodsHubService.setStatus(status, id, apiKey);
            List<String> badResponse = Arrays.asList("ERROR_SQL", "BAD_SERVICE", "BAD_ACTION", "NO_ACTIVATION");
            if (badResponse.contains(responseSetStatus)) {
                return ResponseEntity.badRequest().body(responseSetStatus);
            }
            if (showbalance.isPresent()) {
                responseSetStatus = (String)responseSetStatus + ":balance:" + this.methodsHubService.getBalancer(apiKey);
            }
            return ResponseEntity.ok((Object)responseSetStatus);
        }
        if (action.equals("getPrices")) {
            if (country.isPresent() && !country.get().equals("73")) {
                JSONObject myJson = new JSONObject();
                return ResponseEntity.badRequest().body((Object)myJson);
            }
            Object responseGetPrice = this.methodsHubService.getPrices(service, country);
            if (responseGetPrice == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok((Object)responseGetPrice);
        }
        if (action.equals("getNumbersStatus")) {
            JSONObject response = this.methodsHubService.getNumberStatus(country, operator, smshubEnabled.isPresent());
            return ResponseEntity.ok((Object)response);
        }
        responseAPI = "BAD_ACTION";
        return ResponseEntity.badRequest().body((Object)responseAPI);
    }

    @PostMapping(value={"/conclude/activation/{id}"})
    public ResponseEntity<Object> concludeActivation(@RequestParam(value="api_key") String apiKey, @PathVariable Long id) {
        try {
            if (!this.userService.isValidApiKey(apiKey)) {
                return ResponseEntity.badRequest().build();
            }
            Activation activation = this.activationService.conclude(id, "api conclude activation, NAO PEDIU PARA CANCELAR");
            this.outherService.remove(activation.getChipNumber());
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value={"/cancel/activation/{id}"})
    public synchronized ResponseEntity<Object> cancelActivation(@RequestParam(value="api_key") String apiKey, @PathVariable Long id) {
        try {
            if (!this.userService.isValidApiKey(apiKey)) {
                return ResponseEntity.badRequest().build();
            }
            Activation activation = this.activationRepository.findById(id).get();
            System.out.println("sleep, CANCEL Activation id " + String.valueOf(id) + " " + activation.getStatusBuz().toString());
            int min = 5000;
            int max = 8000;
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(min, max));
            activation = this.activationRepository.findById(id).get();
            System.out.println("after sleep, CANCEL Activation id " + String.valueOf(id) + " " + activation.getStatusBuz().toString());
            if (activation.getStatusBuz() == ActivationStatus.CANCELADA) {
                System.out.println("CANCEL Activation id " + String.valueOf(id) + " JA ESTAVA CANCELADO");
                return ResponseEntity.ok().build();
            }
            System.out.println("continue CANCEL Activation id " + String.valueOf(id) + " ");
            activation = this.activationRepository.findById(id).get();
            this.activationService.cancelActivation(activation, apiKey);
            this.outherService.remove(activation.getChipNumber());
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
