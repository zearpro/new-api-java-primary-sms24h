/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  javax.persistence.EntityManager
 *  javax.persistence.PersistenceContext
 *  javax.persistence.Query
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cache.Cache
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.annotation.Cacheable
 *  org.springframework.context.ApplicationContext
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.apiv2.services;

import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.services.NumerosService;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.core.ServicesHubService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
    Logger logger = LoggerFactory.getLogger(NumerosService.class);
    static ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private ServicesHubService servicesHubService;
    @Autowired
    RedisService redisService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CacheManager cacheManager;
    private final ExecutorService executorService1 = new ThreadPoolExecutor(40, 200, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> findApiCallback(String id) {
        String cachedJson = this.redisService.get("getApiCallbackFromCache", id);
        if (cachedJson == null) {
            JSONObject obj = this.getApiCallbackFromDb(id);
            this.redisService.set("getApiCallbackFromCache", id, obj.toString(), Duration.ofMinutes(10L));
            return obj.toMap();
        }
        return new JSONObject(cachedJson).toMap();
    }

    public JSONObject getApiCallbackFromDb(String id) {
        String objetos = "id, method, url, body_template, header_template";
        String sql = "SELECT " + objetos + " FROM coredb.apiv2_callbacks where id = :id";
        Object formattedQuery = sql;
        Query query = this.entityManager.createNativeQuery(sql);
        query.setParameter("id", (Object)id);
        formattedQuery = ((String)formattedQuery).replace(":id", id);
        List<String> indexMapTrim = Arrays.stream(objetos.split(",")).map(String::trim).collect(Collectors.toList());
        Map<Integer, String> indexedMap = indexMapTrim.stream().collect(Collectors.toMap(index -> indexMapTrim.indexOf(index), value -> value));
        indexedMap.forEach((index, value) -> System.out.println(index + ": " + value));
        this.logger.info("RUN_SQL: " + (String)formattedQuery + ";");
        Object[] result = (Object[])query.getSingleResult();
        JSONObject jsonObject = new JSONObject();
        for (int index2 = 0; index2 < result.length; ++index2) {
            jsonObject.put(indexedMap.getOrDefault(index2, "campo_desconhecido"), result[index2]);
        }
        return jsonObject;
    }

    public Servico getServiceCache(String service) {
        String cacheResult = this.getServiceInCache(service);
        try {
            return (Servico)mapper.readValue(cacheResult, Servico.class);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable(value={"findUserApiType"}, key="#apiKey")
    public String findUserApiType(String apiKey) {
        Optional<User> userOpt = this.userDbRepository.findFirstByApiKey(apiKey);
        if (userOpt.isPresent()) {
            return userOpt.get().getTipo_de_api().name();
        }
        return null;
    }

    @Cacheable(value={"getServiceCache"}, key="#service")
    public String getServiceInCache(String service) {
        try {
            Optional<Servico> result = this.servicesHubService.getService(service);
            if (result.isPresent()) {
                String jsonString = mapper.writeValueAsString((Object)result.get());
                return jsonString;
            }
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clearNumerosDisponiveisCache(int alugado, int ativo, boolean isWa, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp, long startTimeOperacao, Optional<String> agenteId) {
        String cacheKey = String.format("%d,%d,%s,%s,%s,", alugado, ativo, isWa, operadora.orElse(""), filtroDeNumerosParaWhatsApp.toString().replaceAll("[\\[\\]\\s]", ""));
        Cache cache = this.cacheManager.getCache("getNumerosDisponiveisSemFiltrarNumerosPreviosCache");
        cache.evict((Object)cacheKey);
    }

    @Cacheable(value={"getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache"}, key="{#alugado, #ativo, #isWa, #operadora.orElse(''), #filtroDeNumerosParaWhatsApp, #agenteId.orElse('')}")
    public List<String> getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache(int alugado, int ativo, boolean isWa, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp, long startTimeOperacao, Optional<String> agenteId) {
        return this.getNumerosDisponiveisSemFiltrarNumerosPrevios(alugado, ativo, isWa, operadora, filtroDeNumerosParaWhatsApp, Optional.empty(), startTimeOperacao, agenteId);
    }

    @Cacheable(value={"getNumerosDisponiveisSemFiltrarNumerosPreviosCache"}, key="{#alugado, #ativo, #isWa, #operadora.orElse(''), #filtroDeNumerosParaWhatsApp, #agenteId.orElse('')}")
    public List<String> getNumerosDisponiveisSemFiltrarNumerosPreviosCache(int alugado, int ativo, boolean isWa, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp, long startTimeOperacao, Optional<String> agenteId) {
        CacheService self = (CacheService)this.applicationContext.getBean(CacheService.class);
        return self.getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache(alugado, ativo, isWa, operadora, filtroDeNumerosParaWhatsApp, startTimeOperacao, agenteId);
    }

    public List<String> getNumerosDisponiveisSemFiltrarNumerosPrevios(int alugado, int ativo, boolean isWa, Optional<String> operadora, List<String> filtroDeNumerosParaWhatsApp, Optional<String> numeroRecompra, long startTimeOperacao, Optional<String> agenteId) {
        String addWhats = "AND c.vendawhatsapp IN (:valoresVendaWhatsApp)";
        String addOperadora = "AND c.operadora = :operadora";
        String numeroEquals = "AND c.number = :number";
        String agentEquals = "AND c.pc_id in(select id from smshub.Operador where idCliente in (select id from smshub.AgenteUSUARIO where id=:agenteId))";
        String sql = "    SELECT c.number\n    FROM chip_model c\n    WHERE c.alugado = :alugado\n    AND c.ativo = :ativo\n    {0}\n    {1}\n    {2}\n    {3}\n";
        String formatted = MessageFormat.format(sql, filtroDeNumerosParaWhatsApp.size() > 0 && isWa ? addWhats : "", operadora.isPresent() && !operadora.get().toLowerCase().contains("any") ? addOperadora : "", numeroRecompra.isPresent() ? numeroEquals : "", agenteId.isPresent() ? agentEquals : "");
        Query query = this.entityManager.createNativeQuery(formatted);
        String formattedQuery = formatted.replace(":alugado", Utils.stringToSQL(alugado)).replace(":ativo", Utils.stringToSQL(ativo));
        query.setParameter("alugado", (Object)alugado);
        query.setParameter("ativo", (Object)ativo);
        if (filtroDeNumerosParaWhatsApp.size() > 0 && isWa) {
            query.setParameter("valoresVendaWhatsApp", filtroDeNumerosParaWhatsApp);
            formattedQuery = formattedQuery.replace(":valoresVendaWhatsApp", Utils.ListToSql(filtroDeNumerosParaWhatsApp));
        }
        if (operadora.isPresent() && !operadora.get().toLowerCase().contains("any")) {
            query.setParameter("operadora", (Object)operadora.get());
            formattedQuery = formattedQuery.replace(":operadora", Utils.stringToSQL(operadora.get()));
        }
        if (numeroRecompra.isPresent()) {
            query.setParameter("number", (Object)numeroRecompra.get());
            formattedQuery = formattedQuery.replace(":number", Utils.stringToSQL(numeroRecompra.get()));
        }
        if (agenteId.isPresent()) {
            query.setParameter("agenteId", (Object)agenteId.get());
            formattedQuery = formattedQuery.replace(":agenteId", Utils.stringToSQL(agenteId.get()));
        }
        formattedQuery = formattedQuery.replace("[", "(").replace("]", ")").replaceAll(",\\s+", ", ").replace("\"", "'").replaceAll("\n", " ").replaceAll("   ", " ");
        this.logger.info("RUN_SQL: " + formattedQuery + ";");
        long startTimeTrecho = System.nanoTime();
        List<String> numeros = query.getResultList();
        Utils.calcTime(startTimeOperacao, startTimeTrecho, "Tempo para executar a query acima");
        if (numeroRecompra.isPresent()) {
            return numeros.stream().filter(num -> num.toLowerCase().contains(numeroRecompra.get())).collect(Collectors.toList());
        }
        return numeros.stream().collect(Collectors.toList());
    }

    public void setCompraServicoByActivation(String idActivation, String compraId, Duration duration) {
        String key = this.redisService.set("cacheCompraServicoByActivation:", idActivation, compraId, duration);
        this.executorService1.submit(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("key", (Object)key);
                body.put("value", (Object)compraId);
                body.put("ttl", 86400);
                JSONObject jSONObject = Utils.sendHttpRequest("http://3.13.231.176/redis-http/set?token=0ep9p8L5ZE3J", "POST", new HashMap<String, String>(), body.toString());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Long getCompraServicoByActivation(String idActivation) {
        String result = this.redisService.get("cacheCompraServicoByActivation:", idActivation);
        if (result != null) {
            try {
                return Long.valueOf(result);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
