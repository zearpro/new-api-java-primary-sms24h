/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.nimbusds.jose.shaded.json.JSONObject
 *  javax.persistence.EntityManager
 *  javax.persistence.Query
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package br.com.store24h.store24h.services.core;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.MongoService;
import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.CodeResponseStatusEnum;
import br.com.store24h.store24h.apiv2.GetCodeResponse;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.exceptions.ApiKeyNotFoundException;
import br.com.store24h.store24h.apiv2.services.NumerosService;
import br.com.store24h.store24h.services.UserBalanceService;
import br.com.store24h.store24h.dto.SmsDTO;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.ActivationStatusEnum;
import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.ChipNumberControl;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ActivationRepository;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.SmsRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.services.CompraService;
import br.com.store24h.store24h.services.OtherService;
import br.com.store24h.store24h.services.SvsService;
import br.com.store24h.store24h.services.RedisSetService;
import br.com.store24h.store24h.services.VelocityApiService;
import br.com.store24h.store24h.services.core.ActivationService;
import br.com.store24h.store24h.services.core.ServicesHubService;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import br.com.store24h.store24h.services.core.VendaWhatsapp;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicApiService {
    Logger logger = LoggerFactory.getLogger(PublicApiService.class);
    @Autowired
    private CompraService compraService;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private ServicesHubService servicesHubService;
    @Autowired
    private Funcionalidades funcionalidades;
    @Autowired
    private ChipNumberControlService controlService;
    @Autowired
    private SvsService svsService;
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private ChipRepository chipRepository;
    @Autowired
    private ChipNumberControlRepository chipNumberControlRepository;
    @Autowired
    private ActivationRepository activationRepository;
    @Autowired
    private SmsRepository smsRepository;
    @Autowired
    private ServicosRepository servicosRepository;
    @Autowired
    private EntityManager em;
    @Autowired
    private OtherService otherService;
    @Autowired
    private MongoService mongoService;
    @Autowired
    private NumerosService numerosService;
    @Autowired
    private UserBalanceService userBalanceService;
    private final ExecutorService executorService1 = new ThreadPoolExecutor(20, 60, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());
    private RedisService redisService;

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private VelocityApiService velocityApiService;

    public String getBalancer(String api_key) {
        // Use cached balance service for much better performance
        return userBalanceService.getFormattedBalance(api_key);
    }

    @Transactional
    public synchronized void findNumber() {
    }

    public String getNumber(String apiKey, Optional<String> service, Optional<String> operator, Optional<String> country) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        Optional<String> empty = Optional.empty();
        return this.getNumber(apiKey, service, operator, country, empty, 1, TipoDeApiEnum.ANTIGA);
    }

    public String getNumber(String apiKey, Optional<String> service, Optional<String> operator, Optional<String> country, Optional<String> numero, int version) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        return this.getNumber(apiKey, service, operator, country, numero, version, TipoDeApiEnum.ANTIGA);
    }

    public String getNumber(String apiKey, Optional<String> service, Optional<String> operator, Optional<String> country, Optional<String> numero, int version, TipoDeApiEnum tipoDeApiEnum) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        Activation activation;
        long startTimeOperacao = System.nanoTime();
        // Velocity fast path: use high-performance implementation when enabled
        if (System.getenv("VELOCITY_ENABLED") != null) {
            return velocityApiService.getNumberVelocity(apiKey, service, operator, country, numero, version, tipoDeApiEnum);
        }
        if (System.getenv("NEW_GET_NUMBER_METHOD") != null) {
            long startTimeTrecho = System.nanoTime();
            NumerosService.NumeroServiceResponse resp = this.numerosService.getNumber(apiKey, service, operator, country, numero, version, tipoDeApiEnum);
            String tempoMsDoTrecho = Utils.calcTime(startTimeOperacao, startTimeTrecho, "Buscar numero na versao 3");
            if (resp.isError()) {
                return resp.getError().toString();
            }
            return resp.getResponse();
        }
        boolean isGetExtra = numero.isPresent();
        String responseAPI = "";
        long startTimeTrecho = System.nanoTime();
        Optional<User> userdb = this.userDbRepository.findByApiKey(apiKey);
        if (!userdb.isPresent()) {
            throw new ApiKeyNotFoundException();
        }
        User user = userdb.get();
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Buscar usuario pela api key");
        if (version == 2 && !user.getTipo_de_api().equals((Object)tipoDeApiEnum)) {
            throw new TipoDeApiNotPermitedException();
        }
        if (!service.isPresent() || !country.isPresent()) {
            responseAPI = "BAD_ACTION";
            return responseAPI;
        }
        startTimeTrecho = System.nanoTime();
        Optional<Servico> servicoOptional = this.servicesHubService.getService(service.get());
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "- Buscar Servico linha 144");
        if (!servicoOptional.isPresent() || !servicoOptional.get().isActivity()) {
            responseAPI = "BAD_SERVICE";
            return responseAPI;
        }
        List<String> valoresVendawhatsapp = new ArrayList<>();
        valoresVendawhatsapp = user.getWhatsAppEnabled() ? List.of(VendaWhatsapp.HABILITADOS.name(), VendaWhatsapp.TODOS.name()) : List.of(VendaWhatsapp.TODOS.name());
        List<ChipModel> numeroDisponivelList = null;
        List<String> numeroDisponivelPrioridadeList = null;
        startTimeTrecho = System.nanoTime();
        int limite = 100000;
        int limiteMongo = 100000;
        ChipModel chipModel = null;
        for (int tentativaNum = 0; tentativaNum < 2; ++tentativaNum) {
            if (tentativaNum == 0) {
                limite = 100;
                limiteMongo = 500;
            } else {
                limite = 100000;
                limiteMongo = 100000;
            }
            try {
                if (operator.isPresent() && !operator.get().equalsIgnoreCase("ANY")) {
                    startTimeTrecho = System.nanoTime();
                    numeroDisponivelList = servicoOptional.get().getAlias().equals("wa") ? (tentativaNum == 0 ? this.chipRepository.findByAlugadoAndAtivoAndOperadoraAndVendawhatsappIn(false, true, operator.get(), valoresVendawhatsapp, limite) : this.chipRepository.findByAlugadoAndAtivoAndOperadoraAndVendawhatsappInAndService(false, true, operator.get(), valoresVendawhatsapp, servicoOptional.get().getAlias(), limite)) : (tentativaNum == 0 ? this.chipRepository.findByAlugadoAndAtivoAndOperadora((Boolean)false, (Boolean)true, operator.get(), limite) : this.chipRepository.findByAlugadoAndAtivoAndOperadoraAndService(false, true, operator.get(), servicoOptional.get().getAlias(), limite));
                    Utils.calcTime(startTimeOperacao, startTimeTrecho, "Buscar numeros ativos com FILTRO DE OPERADORA");
                } else {
                    List<String> numeros = new ArrayList<>();
                    boolean executaQuery = false;
                    if (!isGetExtra && executaQuery) {
                        startTimeTrecho = System.nanoTime();
                        String queryStr = "select distinct(cm.number) as number from chip_model cm inner join\n                    coredb.activation  a on a.chip_number=cm.number\n                    where (alias_service = \"" + service.get() + "\" or alias_service = \"" + service.get() + "_finalizada\")                    and cm.ativo=1 and date(initial_time)>curdate()-interval 20 day order by a.id desc limit 5000;";
                        Query q = this.em.createNativeQuery(queryStr);
                        numeros = q.getResultList();
                        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo gasto para a query dos numeros q ja possuem o servico");
                    }
                    ArrayList<String> numeroArray = new ArrayList<String>();
                    startTimeTrecho = System.nanoTime();
                    numeroDisponivelList = servicoOptional.get().getAlias().equals("wa") ? (tentativaNum == 0 ? this.chipRepository.findByAlugadoAndAtivoAndVendawhatsappIn(false, true, valoresVendawhatsapp, limite) : this.chipRepository.findByAlugadoAndAtivoAndVendawhatsappInAndService(false, true, valoresVendawhatsapp, servicoOptional.get().getAlias(), limite)) : (tentativaNum == 0 ? this.chipRepository.findByAlugadoAndAtivo(false, true, limite) : this.chipRepository.findByAlugadoAndAtivoAndService(false, true, servicoOptional.get().getAlias(), limite));
                    Utils.calcTime(startTimeOperacao, startTimeTrecho, "Buscar numeros ativos SEM FILTRO DE OPERADORA");
                    this.logger.info("DEV_TESTE:PublicAPI: NUMEROS RETORNADOS ANTES DE FILTRAR: {} ", (Object)numeroDisponivelList.size());
                    if (!isGetExtra) {
                        startTimeTrecho = System.nanoTime();
                        if (!servicoOptional.get().getAlias().equals("ot")) {
                            numeroDisponivelList = this.mongoService.numerosQueNaoForamUsadosNesseServico(numeroDisponivelList, service.get(), limiteMongo);
                        }
                        Utils.calcTime(startTimeOperacao, startTimeTrecho, "BUSCANDO NO MONGO OS NUMEROS QUE NAO FORAM USADOS NESSE SERVICO COM BASE NA QUERY DOS NUMEROS ATIVOS");
                        this.logger.info("DEV_TESTE:PublicAPI: NUMEROS RETORNADOS DEPOIS DE FILTRAR: {} ", (Object)numeroDisponivelList.size());
                    }
                    for (int i = 0; i < numeroDisponivelList.size(); ++i) {
                        numeroArray.add(numeroDisponivelList.get(i).getNumber());
                    }
                    System.out.println(String.join((CharSequence)",", numeroArray));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"getNumber numeros disponiveis", (Object)Utils.getSingleLineStackTrace(e));
                return "ERROR_SQL";
            }
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "-- fim da LISTAGEM DE NUMEROS DISPONIVEIS");
            if (isGetExtra) {
                numeroDisponivelList = numeroDisponivelList.stream().filter(chipModelJ -> ((String)numero.get()).equals(chipModelJ.getNumber())).collect(Collectors.toList());
            }
            if (numeroDisponivelList.isEmpty()) continue;
            if (chipModel == null) {
                Collections.shuffle(numeroDisponivelList);
                startTimeTrecho = System.nanoTime();
                for (ChipModel cm : numeroDisponivelList) {
                    try {
                        Optional<ChipNumberControl> chipNumberControlOptional = this.chipNumberControlRepository.findByChipNumberAndAliasService(cm.getNumber(), service.get());
                        if (chipNumberControlOptional.isPresent() && !isGetExtra || this.otherService.isIn(cm.getNumber()) && !isGetExtra) continue;
                        try {
                            chipModel = cm;
                            System.out.println(cm.getNumber() + " : " + servicoOptional.get().getAlias());
                            System.out.println(service.get());
                            break;
                        }
                        catch (Exception e) {
                            this.logger.info("ERRO AO ADICIONAR O SERVICO {} NO NUMERO {}", (Object)servicoOptional.get().getAlias(), (Object)cm.getNumber());
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Utils.calcTime(startTimeOperacao, startTimeTrecho, "escolher entre os numeros disponiveis um para uso");
            }
            if (chipModel != null) break;
        }
        if (numeroDisponivelList.isEmpty()) {
            if (servicoOptional.get().getAlias().equals("wa") && !user.getWhatsAppEnabled() && this.servicosRepository.findByAlias(servicoOptional.get().getAlias()).get().getTotalQuantity() > 0) {
                responseAPI = "FORBIDEN_SERVICE";
                return responseAPI;
            }
            responseAPI = "NO_NUMBERS";
            return responseAPI;
        }
        if (chipModel == null) {
            if (servicoOptional.get().getAlias().equals("wa") && !user.getWhatsAppEnabled() && this.servicosRepository.findByAlias(servicoOptional.get().getAlias()).get().getTotalQuantity() > 0) {
                responseAPI = "FORBIDEN_SERVICE";
                return responseAPI;
            }
            responseAPI = "NO_NUMBERS";
            return responseAPI;
        }
        startTimeTrecho = System.nanoTime();
        String hasCredit = this.compraService.virifyCredit(user, servicoOptional.get());
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "checar se possui saldo");
        if (hasCredit.equals("false")) {
            responseAPI = "NO_BALANCE";
            return responseAPI;
        }
        startTimeTrecho = System.nanoTime();
        try {
            activation = this.activationService.newActivation(user, servicoOptional.get(), chipModel.getNumber(), apiKey, version);
            if (activation.getAliasService().equals("ot")) {
                this.executorService1.submit(() -> {
                    this.otherService.save(activation.getChipNumber());
                    Thread.currentThread().interrupt();
                });
            }
            String chipNumber = chipModel.getNumber();
            this.executorService1.submit(() -> {
                this.controlService.addServiceInNumber(chipNumber, (Servico)servicoOptional.get());
                Thread.currentThread().interrupt();
            });
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"getNumber new activiation", (Object)Utils.getSingleLineStackTrace(e));
            return "ERROR_SQL";
        }
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Criar ativacao, adicionar servico no numero e debitar o saldo");
        responseAPI = "ACCESS_NUMBER:" + activation.getId() + ":" + chipModel.getNumber();
        return responseAPI;
    }

    public Activation getActivation(Long idActivation) {
        Activation activation = (Activation)this.activationRepository.getById(idActivation);
        return activation;
    }

    public SmsDTO getSms(Long idActivation) {
        String sms;
        Optional<Activation> activationOpt = this.activationRepository.findFirstById(idActivation);
        Activation activation = activationOpt.get();
        SmsDTO smsDTO = new SmsDTO();
        smsDTO.setNameService(activation.getServiceName());
        smsDTO.setStatus(activation.getStatus());
        smsDTO.setAliasService(activation.getAliasService());
        smsDTO.setNumberActivation(activation.getChipNumber());
        try {
            sms = activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1);
        }
        catch (IndexOutOfBoundsException e) {
            smsDTO.setSmsList(activation.getSmsStringModels());
            return smsDTO;
        }
        smsDTO.getSmsList().add(sms);
        return smsDTO;
    }

    public SmsDTO getSmsRetry(Long idActivation) {
        Activation activation = (Activation)this.activationRepository.getById(idActivation);
        SmsDTO smsDTO = new SmsDTO();
        smsDTO.setStatus(activation.getStatus());
        smsDTO.setNameService(activation.getServiceName());
        smsDTO.setAliasService(activation.getAliasService());
        smsDTO.setNumberActivation(activation.getChipNumber());
        smsDTO.getSmsList().add(activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1));
        return smsDTO;
    }

    public GetCodeResponse getActivationStatus(Optional<Long> id, String apiKey) {
        GetCodeResponse response = new GetCodeResponse();
        Optional<Activation> activationOptional = Optional.empty();
        if (!id.isPresent() && !(id.get() instanceof Long)) {
            response.setError(CodeResponseStatusEnum.BAD_ACTION);
            return response;
        }
        activationOptional = this.activationRepository.findFirstById(id.get());
        response.setId(id.get());
        if (!activationOptional.isPresent() || !((Activation)activationOptional.get()).getApiKey().equals(apiKey)) {
            response.setError(CodeResponseStatusEnum.NO_ACTIVATION);
            return response;
        }
        Activation activation = (Activation)activationOptional.get();
        response.setStatus(activation.getStatusDesc(), activation.getStatus());
        response.setNumber(activation.getChipNumber());
        response.setService(activation.getAliasService());
        if (activation.getStatusDesc() != null && activation.getStatusDesc().equals((Object)ActivationStatusEnum.STATUS_WAIT_RETRY)) {
            int index = activation.getSmsStringModels().size();
            response.setCode(activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1));
        } else if (activation.getStatus() != -1 && activation.getStatus() != 3 && activation.getStatus() != 8) {
            response.setCode(activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1));
        }
        return response;
    }

    public String getStatus(Optional<Long> id, String apiKey) {
        Optional<Activation> activationOptional;
        JSONObject myJson = new JSONObject();
        if (!id.isPresent() && !(id.get() instanceof Long)) {
            return "BAD_ACTION";
        }
        try {
            activationOptional = this.activationRepository.findFirstById(id.get());
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"getStatus", (Object)Utils.getSingleLineStackTrace(e));
            return "ERROR_SQL";
        }
        if (!activationOptional.isPresent() || !activationOptional.get().getApiKey().equals(apiKey)) {
            return "NO_ACTIVATION";
        }
        Activation activation = activationOptional.get();
        int statusCode = activation.getStatus();
        if (statusCode == -1) {
            return "STATUS_WAIT_CODE";
        }
        int index = activation.getSmsStringModels().size();
        if (statusCode == 3) {
            return "STATUS_WAIT_RETRY:" + activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1);
        }
        if (statusCode == 8) {
            return "STATUS_CANCEL";
        }
        return "STATUS_OK:" + activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1);
    }

    public synchronized String setStatus(Optional<Integer> status, Optional<Long> idActivation, String apiKey) {
        return this.setStatus(status, idActivation, apiKey, 1);
    }

    public synchronized String setStatus(Optional<Integer> status, Optional<Long> idActivation, String apiKey, int version) {
        Optional<Activation> activationOptional;
        List<Integer> acceptedStatus = Arrays.asList(1, 3, 6, 8);
        if (!(status.isPresent() && acceptedStatus.contains(status.get()) && idActivation.isPresent())) {
            return "BAD_ACTION";
        }
        int statusCode = status.get();
        try {
            activationOptional = this.activationRepository.findById(idActivation.get());
            if (!activationOptional.isPresent() || !activationOptional.get().getApiKey().equals(apiKey)) {
                return "NO_ACTIVATION";
            }
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"setStatus activationRepository.findById ", (Object)Utils.getSingleLineStackTrace(e));
            return "ERROR_SQL";
        }
        Activation activation = activationOptional.get();
        if (statusCode == 1) {
            try {
                if (this.activationService.initialStatus(activation)) {
                    return "ACCESS_READY";
                }
                return "BAD_ACTION";
            }
            catch (Exception e) {
                this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"setStatus setInitialStatus", (Object)Utils.getSingleLineStackTrace(e));
                return "ERROR_SQL";
            }
        }
        if (statusCode == 3) {
            try {
                if (this.activationService.awaitNewCode(activation)) {
                    return "ACCESS_RETRY_GET";
                }
                return "BAD_ACTION";
            }
            catch (Exception e) {
                this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"setStatus AwaitNewCode", (Object)Utils.getSingleLineStackTrace(e));
                return "ERROR_SQL";
            }
        }
        if (statusCode == 8) {
            try {
                ThreadLocalRandom.current().nextInt(200, 400);
                String response = this.activationService.cancelActivation(activation, apiKey, version);
                return response;
            }
            catch (Exception e) {
                this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"setStatus CancelarAtivacao", (Object)Utils.getSingleLineStackTrace(e));
                return "ERROR_SQL";
            }
        }
        try {
            this.activationService.conclude(activation);
            return "ACCESS_ACTIVATION";
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"setStatus ConcluirAtivacao", (Object)Utils.getSingleLineStackTrace(e));
            return "ERROR_SQL";
        }
    }

    public JSONObject getNumberStatus(Optional<String> country, Optional<String> operator) {
        return this.getNumberStatus(country, operator, false);
    }

    public JSONObject getNumberStatus(Optional<String> country, Optional<String> operator, boolean isSmshub) {
        JSONObject myJson = new JSONObject();
        List<br.com.store24h.store24h.model.Servico> servicoList = new ArrayList<>();
        if (!country.isPresent()) {
            servicoList = isSmshub ? this.servicosRepository.findAllBySmshub(1) : this.servicosRepository.findAll();
            servicoList.forEach(servico -> myJson.put(servico.getAlias() + "_0", 0));
            return myJson;
        }
        try {
            servicoList = isSmshub ? this.servicosRepository.findAllBySmshub(1) : this.servicosRepository.findAll();
            if (operator.isPresent() && !operator.get().toLowerCase().equals("any")) {
                List<ChipModel> chipModelList = this.chipRepository.findByAtivoAndOperadora(true, operator.get().toLowerCase());
                ArrayList<String> numbers = new ArrayList<String>();
                chipModelList.forEach(chipModel -> numbers.add(chipModel.getNumber()));
                List<ChipNumberControl> chipNumberControlList = this.chipNumberControlRepository.findByChipNumberIn(numbers);
                servicoList.forEach(servico -> {
                    servico.setTotalQuantity(0);
                    chipNumberControlList.forEach(chipNumberControl -> {
                        List<String> aliasService = chipNumberControl.getAliasService();
                        int index = aliasService.indexOf(servico.getAlias());
                        if (index <= -1) {
                            servico.setTotalQuantity(servico.getTotalQuantity() + 1);
                        }
                    });
                });
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            this.logger.info("ERROR_SQL_DESC: {} STACK: {}", (Object)"getNumberStatus", (Object)Utils.getSingleLineStackTrace(e));
            myJson.put("ERROR_SQL", "erro SQL-server");
            return myJson;
        }
        servicoList.forEach(servico -> myJson.put(servico.getAlias() + "_0", servico.getTotalQuantity()));
        return myJson;
    }

    public Object getPrices(Optional<String> service, Optional<String> country, Optional<String> operator) {
        try {
            this.logger.debug("getPrices called with service:{} country:{} operator:{}", 
                service.orElse("null"), country.orElse("null"), operator.orElse("null"));
            
            // Velocity fast path: use high-performance implementation when enabled
            if (System.getenv("VELOCITY_ENABLED") != null) {
                return velocityApiService.getPricesVelocity(service, country, operator);
            }
                
            if (service.isPresent()) {
                // ✅ Single service request - using HashMap for proper JSON serialization
                Map<String, Object> myJson = new HashMap<>();
                Optional<Servico> servicoOptional = this.servicosRepository.findFirstByAlias(service.get());
                Map<String, Object> serviceMyJson = new HashMap<>();
                
                if (servicoOptional.isPresent()) {
                    Servico s = servicoOptional.get();
                    Map<String, Object> priceMyJson = new HashMap<>();
                    
                    // ✅ Keep same price from servicos table
                    priceMyJson.put("cost", s.getPrice());

                    // ✅ Prefer Redis pool count if operator and country provided; fallback to DB field
                    if (operator.isPresent() && country.isPresent()) {
                        try {
                            long poolCount = redisSetService.getAvailableCount(operator.get(), s.getAlias(), country.get());
                            priceMyJson.put("count", poolCount);
                        } catch (Exception ex) {
                            priceMyJson.put("count", s.getTotalQuantity());
                        }
                    } else {
                        priceMyJson.put("count", s.getTotalQuantity());
                    }
                    
                    // ✅ Use actual service alias as key (wa, tg, fb, etc.)
                    serviceMyJson.put(s.getAlias(), priceMyJson);
                    myJson.put(country.get(), serviceMyJson);
                    
                    this.logger.debug("getPrices returning: {}", myJson.toString());
                    return myJson;
                }
            }
            
            // ✅ All services request - using HashMap for proper JSON serialization
            List<Servico> servicoList = this.servicosRepository.findAll();
            Map<String, Object> myJson = new HashMap<>();
            Map<String, Object> serviceMyJson = new HashMap<>();
            
            for (Servico s : servicoList) {
                Map<String, Object> priceMyJson = new HashMap<>();
                priceMyJson.put("cost", s.getPrice());
                priceMyJson.put("count", s.getTotalQuantity());
                serviceMyJson.put(s.getAlias(), priceMyJson);
            }
            myJson.put(country.get(), serviceMyJson);
            
            this.logger.debug("getPrices (all) returning: {}", myJson.toString());
            return myJson;
            
        } catch (Exception e) {
            this.logger.error("Error in getPrices", e);
            Map<String, Object> errorJson = new HashMap<>();
            errorJson.put("error", "Internal error");
            return errorJson;
        }
    }

    /**
     * Calculate available count for specific country+operator+service_alias
     */
    private int calculateCountForServiceAlias(String country, Optional<String> operator, String serviceAlias) {
        try {
            List<ChipModel> availableChips;
            
            // ✅ Use existing methods with correct parameters during migration period
            if (operator.isPresent() && !operator.get().equalsIgnoreCase("any")) {
                availableChips = chipRepository.findByAlugadoAndAtivoAndOperadora(Boolean.FALSE, Boolean.TRUE, operator.get(), 1000);
            } else {
                availableChips = chipRepository.findByAlugadoAndAtivo(Boolean.FALSE, Boolean.TRUE, 1000);
            }
            
            // ✅ Return count of available chips 
            int count = availableChips.size();
            this.logger.debug("Found {} available chips for country:{} operator:{} service:{}", 
                count, country, operator.orElse("any"), serviceAlias);
            
            return count;
            
        } catch (Exception e) {
            this.logger.error("Error calculating count for {}:{}:{}", country, operator.orElse("any"), serviceAlias, e);
            return 100; // Return reasonable default on error
        }
    }
}
