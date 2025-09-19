/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  javax.persistence.EntityManager
 *  javax.persistence.PersistenceContext
 *  javax.persistence.Query
 *  org.apache.commons.collections4.ListUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cache.CacheManager
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.apiv2.services;

import br.com.store24h.store24h.BigDecimalPercentages;
import br.com.store24h.store24h.MongoService;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.exceptions.ApiKeyNotFoundException;
import br.com.store24h.store24h.apiv2.model.ChipNumberControlAliasServiceDTO;
import br.com.store24h.store24h.apiv2.repository.ChipNumberControlAliasRepository;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.apiv2.services.SmsReplicateService;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.services.CompraService;
import br.com.store24h.store24h.services.OtherService;
import br.com.store24h.store24h.services.core.ActivationService;
import br.com.store24h.store24h.services.core.ServicesHubService;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import br.com.store24h.store24h.services.core.VendaWhatsapp;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class NumerosService {
    Logger logger = LoggerFactory.getLogger(NumerosService.class);
    @Autowired
    private CompraService compraService;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private ChipNumberControlService controlService;
    @Autowired
    private CacheService cacheService;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private OtherService otherService;
    @Autowired
    private MongoService mongoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ChipRepository chipRepository;
    @Autowired
    private ChipNumberControlRepository chipNumberControlRepository;
    @Autowired
    private ChipNumberControlAliasRepository chipNumberControlAliasRepository;
    @Autowired
    private ServicesHubService servicesHubService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private SmsReplicateService smsReplicateService;
    @Autowired
    private UserDbRepository userDbRepository;
    Executor executor = Executors.newFixedThreadPool(10);
    Executor executor400 = Executors.newFixedThreadPool(200);
    private final ExecutorService executorService1 = new ThreadPoolExecutor(20, 100, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());

    public User findUserByApiKey(String apiKey) {
        Optional<User> usr = this.userDbRepository.findByApiKey(apiKey);
        return usr.isPresent() ? usr.get() : null;
    }

    public List<List<String>> getActivationsValids(String apiKey) {
        String sql = "    SELECT a.id, a.chip_number\n    FROM activation a\n    WHERE a.api_key = :apiKey\n    AND a.status in(8,6)\n    and a.initial_time > now()-interval 200 minute\n    ;\n";
        Query query = this.entityManager.createNativeQuery(sql);
        query.setParameter("apiKey", (Object)apiKey);
        List<Object[]> results = query.getResultList();
        ArrayList<List<String>> jsonResults = new ArrayList<List<String>>();
        for (Object[] row : results) {
            jsonResults.add(List.of(row[0].toString(), row[1].toString()));
        }
        return jsonResults;
    }

    public NumeroServiceResponse getNumber(String apiKey, Optional<String> service, Optional<String> operator, Optional<String> country, Optional<String> numero, int version, TipoDeApiEnum tipoDeApiEnum) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        long startTimeOperacao = System.nanoTime();
        return this.getNumber(apiKey, service, operator, country, numero, version, tipoDeApiEnum, startTimeOperacao);
    }

    public List<ChipModel> removeNumerosQueJaPossuemOServico(List<String> numeros, String service, boolean isExtra, long startTimeOperacao) {
        List<ChipNumberControlAliasServiceDTO> numerosEServicesControl;
        long startTimeTrecho = System.nanoTime();
        int tentativa = 1;
        if (tentativa == 3) {
            ArrayList<ChipModel> rlist = new ArrayList<ChipModel>();
            return rlist;
        }
        if (tentativa == 2) {
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] START  chipNumberControlAliasRepository.findByChipNumberInAndAliasService(numeros, service, LocalDateTime.now().minusMinutes(20))");
            numerosEServicesControl = this.chipNumberControlAliasRepository.findByChipNumberInAndAliasService(numeros, service, LocalDateTime.now().minusMinutes(20L));
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] END  chipNumberControlAliasRepository.findByChipNumberInAndAliasService(numeros, service, LocalDateTime.now().minusMinutes(20))");
        } else {
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] START findByChipNumberInAndAliasService(numeros, service)");
            numerosEServicesControl = this.chipNumberControlAliasRepository.findByChipNumberInAndAliasService(numeros, service);
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "]  END findByChipNumberInAndAliasService(numeros, service)");
        }
        startTimeTrecho = System.nanoTime();
        Map<String, ChipNumberControlAliasServiceDTO> chipNumberMap = numerosEServicesControl.stream().collect(Collectors.toMap(ChipNumberControlAliasServiceDTO::getNumero, chip -> chip, (existing, replacement) -> existing));
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] convert numerosEServicesControl to chipNumberMap");
        List<CompletableFuture<Optional<ChipModel>>> futures = numeros.stream().map(numero -> CompletableFuture.supplyAsync(() -> {
            Optional<ChipModel> chip = Optional.empty();
            if (!chipNumberMap.containsKey(numero) || isExtra) {
                chip = this.chipRepository.findByNumber(numero);
            }
            return chip;
        }, numeros.size() < 401 ? this.executor400 : this.executor)).collect(Collectors.toList());
        startTimeTrecho = System.nanoTime();
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] START List<ChipModel> resultado = futures");
        List<ChipModel> resultado = futures.stream().map(CompletableFuture::join).flatMap(optional -> optional.stream()).collect(Collectors.toList());
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [inside(removeNumerosQueJaPossuemOServico)] [TENTATIVA " + String.valueOf(tentativa) + "] END List<ChipModel> resultado = futures");
        return resultado;
    }

    public NumeroServiceResponse getNumber(String apiKey, Optional<String> service, Optional<String> operator, Optional<String> country, Optional<String> numeroRecompra, int version, TipoDeApiEnum tipoDeApiEnum, long startTimeOperacao) throws ApiKeyNotFoundException, TipoDeApiNotPermitedException {
        long startTimeTrecho = System.nanoTime();
        boolean isGetExtra = numeroRecompra.isPresent();
        String queryStrategy = "desc";
        User userCacheOpt = this.findUserByApiKey(apiKey);
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo para buscar o usuario no cache");
        if (userCacheOpt == null) {
            throw new ApiKeyNotFoundException();
        }
        User userCache = userCacheOpt;
        if (version == 2 && !userCache.getTipo_de_api().equals((Object)tipoDeApiEnum)) {
            throw new TipoDeApiNotPermitedException();
        }
        if (!service.isPresent() || !country.isPresent()) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.BAD_ACTION);
        }
        if (country.get().isEmpty() || Double.parseDouble(country.get()) <= 0) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.NO_NUMBERS);
        }
        Servico serviceCache = this.cacheService.getServiceCache(service.get());
        if (serviceCache == null || !serviceCache.isActivity()) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.BAD_SERVICE);
        }
        List<String> filtroDeNumerosParaWhatsApp = new ArrayList<String>();
        filtroDeNumerosParaWhatsApp = userCache.getWhatsAppEnabled() ? List.of(VendaWhatsapp.HABILITADOS.name(), VendaWhatsapp.TODOS.name()) : List.of(VendaWhatsapp.TODOS.name());
        Optional<User> userRealtime = this.userDbRepository.findFirstByApiKey(userCache.getApiKey());
        if (!userRealtime.isPresent()) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.BAD_ACTION);
        }
        User user = userRealtime.get();
        ArrayList<String> numerosDisponiveisParaEsteServico = new ArrayList<String>();
        String agenteId = user.getAgente();
        queryStrategy = "s2";
        startTimeTrecho = System.nanoTime();
        if (numeroRecompra.isPresent()) {
            numerosDisponiveisParaEsteServico.addAll(this.cacheService.getNumerosDisponiveisSemFiltrarNumerosPrevios(0, 1, serviceCache.getAlias().equals("wa"), operator, filtroDeNumerosParaWhatsApp, numeroRecompra, startTimeOperacao, Optional.ofNullable(agenteId)));
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo para getNumerosDisponiveisSemFiltrarNumerosPrevios");
        } else {
            numerosDisponiveisParaEsteServico.addAll(this.cacheService.getNumerosDisponiveisSemFiltrarNumerosPreviosCache(0, 1, serviceCache.getAlias().equals("wa"), operator, filtroDeNumerosParaWhatsApp, startTimeOperacao, Optional.ofNullable(agenteId)));
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo para getNumerosDisponiveisSemFiltrarNumerosPreviosCache");
        }
        if (serviceCache.getAlias().equals("wa") && !userCache.getWhatsAppEnabled() && numerosDisponiveisParaEsteServico.size() == 0) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.FORBIDEN_SERVICE);
        }
        if (isGetExtra && numerosDisponiveisParaEsteServico.size() == 0) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.NO_NUMBERS);
        }
        startTimeTrecho = System.nanoTime();
        Optional<ChipModel> chipModelOptional = this.escolherChip(numerosDisponiveisParaEsteServico, serviceCache.getAlias(), isGetExtra);
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo para escolher um chip");
        if (serviceCache.getAlias().equals("wa") && !userCache.getWhatsAppEnabled() && chipModelOptional.isEmpty() && this.servicesHubService.getService(serviceCache.getAlias()).get().getTotalQuantity() > 0) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.FORBIDEN_SERVICE);
        }
        if (chipModelOptional.isEmpty()) {
            return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.NO_NUMBERS);
        }
        startTimeTrecho = System.nanoTime();
        try {
            BigDecimal valor = serviceCache.getPrice();
            BigDecimal porcentagem = user.getPorcentagemPagamento();
            BigDecimal novopreco = BigDecimalPercentages.percentOf(porcentagem, valor);
            serviceCache.setPrice(novopreco);
        }
        catch (Exception valor) {
            // empty catch block
        }
        if (!apiKey.equals("2bce230ccb852582f693e803def487aa")) {
            String hasCredit = this.compraService.virifyCredit(user, serviceCache);
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "checar se possui saldo");
            if (hasCredit.equals("false")) {
                return new NumeroServiceResponse(NumerosServiceResponseErrorEnum.NO_BALANCE);
            }
        }
        ChipModel chipModel = chipModelOptional.get();
        startTimeTrecho = System.nanoTime();
        Activation activation = this.activationService.newActivation(user, serviceCache, chipModel.getNumber(), apiKey, version);
        String chipNumber = chipModel.getNumber();
        this.executorService1.execute(() -> {
            this.controlService.addServiceInNumber(chipNumber, serviceCache);
            try {
                if (Utils.isHomolog() && user.getCallbackUrlId() != null && user.getTipo_de_api().equals((Object)TipoDeApiEnum.SISTEMAS)) {
                    this.logger.info("SMS_REPLICATE ADD");
                    this.smsReplicateService.addNumberToReplicateInApi(activation, user);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            Thread.currentThread().interrupt();
        });
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "criou a ativacao e retornar");
        return new NumeroServiceResponse(activation, chipModel, queryStrategy);
    }

    public IsCachedStatusResponse isGetNumerosDisponiveisSemFiltrarNumerosPreviosCached(int alugado, int ativo, boolean isWa, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp) {
        String cacheKey = String.format("%d,%d,%s,%s,%s", alugado, ativo, isWa, operadora.orElse(""), filtroDeNumerosParaWhatsApp.toString().replaceAll("[\\[\\]\\s]", ""));
        boolean resp = this.cacheManager.getCache("getNumerosDisponiveisSemFiltrarNumerosPreviosCache").get((Object)cacheKey) != null;
        return new IsCachedStatusResponse(cacheKey, resp);
    }

    private Optional<ChipModel> escolherChip(List<String> numerosDisponiveisParaEsteServico, String service, boolean isExtra) {
        long startTimeOperacao = System.nanoTime();
        long startTimeTrecho = System.nanoTime();
        Collections.shuffle(numerosDisponiveisParaEsteServico);
        int sizeToChunks = 100;
        if (numerosDisponiveisParaEsteServico.size() < 400) {
            sizeToChunks = 400;
        }
        List<List<String>> chunksNumeros = ListUtils.partition(numerosDisponiveisParaEsteServico, (int)sizeToChunks);
        Utils.calcTime(startTimeOperacao, startTimeTrecho, String.format("      [escolha do chip] - dividido em %s parts de %s / %s total", String.valueOf(chunksNumeros.size()), sizeToChunks, numerosDisponiveisParaEsteServico.size()));
        for (List<String> chunkNumero : chunksNumeros) {
            startTimeTrecho = System.nanoTime();
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [chunkNumero] start removeNumerosQueJaPossuemOServico");
            List<ChipModel> disponiveis = this.removeNumerosQueJaPossuemOServico(chunkNumero, service, isExtra, startTimeOperacao);
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [chunkNumero] end removeNumerosQueJaPossuemOServico");
            startTimeTrecho = System.nanoTime();
            Collections.shuffle(disponiveis);
            Utils.calcTime(startTimeOperacao, startTimeTrecho, "      [chunkNumero] shuffle nos disponiveis");
            List chipNumbersDisponiveis = disponiveis.stream().map(ChipModel::getNumber).collect(Collectors.toList());
            ArrayList<String> filteredList = new ArrayList<String>(chunkNumero);
            filteredList.removeAll(chipNumbersDisponiveis);
            System.out.println(Utils.ListToSql(filteredList));
            Iterator<ChipModel> iterator = disponiveis.iterator();
            if (!iterator.hasNext()) continue;
            ChipModel chipModel = iterator.next();
            return Optional.of(chipModel);
        }
        return Optional.empty();
    }

    public List<String> getNumerosDisponiveis(int alugado, int ativo, String service, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp, Optional<String> numeroRecompra) {
        String addWhats = "AND c.vendawhatsapp IN (:valoresVendaWhatsApp)";
        String addOperadora = "AND c.operadora IN (:operadora)";
        String numeroNotIN = "AND c.number NOT IN (\n                SELECT cm.number \n                FROM chip_number_control nc\n                INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n                INNER JOIN chip_model cm ON cm.number = nc.chip_number\n                WHERE al.alias_service = :service \n                AND cm.ativo = 1 \n            )";
        String numeroEquals = "AND c.number = :number";
        String sql = "    SELECT c.number\n    FROM chip_model c\n    WHERE c.alugado = :alugado\n    AND c.ativo = :ativo\n    {0}\n    {1}\n    {2}\n";
        Object[] objectArray = new Object[3];
        objectArray[0] = filtroDeNumerosParaWhatsApp.size() > 0 && service.equals("wa") ? addWhats : "";
        Object object = objectArray[1] = operadora.isPresent() && !operadora.get().toLowerCase().contains("any") ? addOperadora : "";
        objectArray[2] = numeroRecompra.isPresent() ? numeroEquals : (service.equals("ot") ? "" : numeroNotIN);
        String formatted = MessageFormat.format(sql, objectArray);
        Query query = this.entityManager.createNativeQuery(formatted);
        String formattedQuery = formatted.replace(":alugado", Utils.stringToSQL(alugado)).replace(":ativo", Utils.stringToSQL(ativo));
        query.setParameter("alugado", (Object)alugado);
        query.setParameter("ativo", (Object)ativo);
        if (filtroDeNumerosParaWhatsApp.size() > 0 && service.equals("wa")) {
            query.setParameter("valoresVendaWhatsApp", filtroDeNumerosParaWhatsApp);
            formattedQuery = formattedQuery.replace(":valoresVendaWhatsApp", Utils.ListToSql(filtroDeNumerosParaWhatsApp));
        }
        if (!service.equals("ot")) {
            query.setParameter("service", (Object)service);
            formattedQuery = formattedQuery.replace(":service", Utils.stringToSQL(service));
        }
        if (operadora.isPresent() && !operadora.get().toLowerCase().contains("any")) {
            query.setParameter("operadora", (Object)operadora.get());
            formattedQuery = formattedQuery.replace(":operadora", Utils.stringToSQL(operadora.get()));
        }
        if (numeroRecompra.isPresent()) {
            query.setParameter("number", (Object)numeroRecompra.get());
            formattedQuery = formattedQuery.replace(":number", Utils.stringToSQL(numeroRecompra.get()));
        }
        formattedQuery = formattedQuery.replace("[", "(").replace("]", ")").replaceAll(",\\s+", ", ").replace("\"", "'").replaceAll("\n", " ").replaceAll("   ", " ");
        this.logger.info("RUN_SQL: " + formattedQuery + ";");
        List<String> numeros = query.getResultList();
        if (numeroRecompra.isPresent()) {
            return numeros.stream().filter(num -> num.toLowerCase().contains(numeroRecompra.get())).collect(Collectors.toList());
        }
        return numeros.stream().collect(Collectors.toList());
    }

    public List<String> dev_numerosDisponiveis(int alugado, int ativo, String service, Optional<String> operadora, List<String> valoresVendaWhatsApp) {
        String addWhats = "AND c.vendawhatsapp IN (:valoresVendaWhatsApp)";
        String addOperadora = "AND c.operadora IN (:operadora)";
        String sql = "    SELECT c.*\n    FROM chip_model c\n    WHERE c.alugado = :alugado\n    AND c.ativo = :ativo\n    {0}\n    {1}\n    AND c.number NOT IN (\n        SELECT cm.number\n        FROM chip_number_control nc\n        INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n        INNER JOIN chip_model cm ON cm.number = nc.chip_number\n        WHERE al.alias_service = :service\n        AND cm.ativo = 1\n    );\n";
        String formatted = MessageFormat.format(sql, valoresVendaWhatsApp.size() > 0 ? addWhats : "", operadora.isPresent() ? addOperadora : "");
        Query query = this.entityManager.createNativeQuery(formatted);
        query.setParameter("alugado", (Object)alugado);
        query.setParameter("ativo", (Object)ativo);
        query.setParameter("service", (Object)service);
        if (valoresVendaWhatsApp.size() > 0) {
            query.setParameter("valoresVendaWhatsApp", valoresVendaWhatsApp);
        }
        if (operadora.isPresent()) {
            query.setParameter("operadora", (Object)operadora.get());
        }
        List<Object[]> results = query.getResultList();
        ArrayList<String> jsonResults = new ArrayList<String>();
        for (Object[] row : results) {
            jsonResults.add(row[4].toString());
        }
        return jsonResults;
    }

    public class NumeroServiceResponse {
        private final Activation activation;
        private final ChipModel chipModel;
        private final String queryStrategy;
        NumerosServiceResponseErrorEnum error = null;

        public NumeroServiceResponse(Activation activation, ChipModel chipModel, String queryStrategy) {
            this.activation = activation;
            this.chipModel = chipModel;
            this.queryStrategy = queryStrategy;
            this.error = null;
        }

        public NumeroServiceResponse(NumerosServiceResponseErrorEnum error) {
            this.activation = null;
            this.chipModel = null;
            this.error = error;
            this.queryStrategy = null;
        }

        public NumerosServiceResponseErrorEnum getError() {
            if (this.error != null) {
                return this.error;
            }
            return NumerosServiceResponseErrorEnum.UNKNOWN_ERROR;
        }

        public boolean isError() {
            return this.error != null || this.activation == null || this.chipModel == null;
        }

        public String getQueryStrategy() {
            return this.queryStrategy;
        }

        public String getResponse() {
            return "ACCESS_NUMBER:" + this.activation.getId() + ":" + this.chipModel.getNumber();
        }
    }

    public static enum NumerosServiceResponseErrorEnum {
        BAD_ACTION,
        NO_NUMBERS,
        UNKNOWN_ERROR,
        BAD_SERVICE,
        NO_BALANCE,
        FORBIDEN_SERVICE;

    }

    public class IsCachedStatusResponse {
        private final boolean resp;
        private final String key;

        public IsCachedStatusResponse(String key, boolean resp) {
            this.key = key;
            this.resp = resp;
        }

        public boolean isResp() {
            return this.resp;
        }

        public String getKey() {
            return this.key;
        }

        public String toString() {
            return "IsCachedStatusResponse{resp=" + this.resp + ", key='" + this.key + "'}";
        }
    }
}
