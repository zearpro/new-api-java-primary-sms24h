/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  javax.annotation.PostConstruct
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.core.env.ConfigurableEnvironment
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.mongodb.core.MongoTemplate
 *  org.springframework.data.mongodb.core.index.Index
 *  org.springframework.data.mongodb.core.index.IndexDefinition
 *  org.springframework.data.mongodb.core.query.Criteria
 *  org.springframework.data.mongodb.core.query.CriteriaDefinition
 *  org.springframework.data.mongodb.core.query.Query
 *  org.springframework.stereotype.Component
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.model.ChipModel;
import com.google.gson.Gson;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MongoService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ConfigurableEnvironment environment;
    Logger logger = LoggerFactory.getLogger(MongoService.class);

    private boolean isBypassEnabled() {
        try {
            String bypass = System.getenv("MONGO_BYPASS");
            return bypass != null && ("1".equals(bypass) || "true".equalsIgnoreCase(bypass));
        } catch (Exception e) {
            return false;
        }
    }

    @PostConstruct
    public void createTTLIndex() {
        if (isBypassEnabled()) {
            // Skip TTL index creation when bypassing Mongo in hot paths
            return;
        }
        this.mongoTemplate.indexOps("numbers_to_callback").ensureIndex((IndexDefinition)new Index().on("createdAt", Sort.Direction.ASC).expire(Duration.ofHours(1L)));
    }

    public List<String> numerosComOServico(String servico) {
        if (isBypassEnabled()) {
            // When bypassing, pretend none were used so selection can proceed
            return new ArrayList<>();
        }
        Query query = new Query();
        this.logger.info("DEV_TESTE: buscando numeros que ja ativou o servico {} ", (Object)servico);
        query.addCriteria((CriteriaDefinition)Criteria.where((String)"servico").is((Object)servico));
        List<Map> compra_numero_servico = this.mongoTemplate.find(query, Map.class, "compras_ativadas");
        ArrayList<String> numeros = new ArrayList<String>();
        for (Map compra : compra_numero_servico) {
            numeros.add((String)compra.get("numero"));
        }
        this.logger.info("DEV_TESTE: numeros que ja ativou o servico {} e devo ignorar {} ", (Object)servico, numeros);
        return numeros;
    }

    public boolean possoUsar(String api_key, String numero, String servico) {
        if (isBypassEnabled()) {
            // Allow usage by default when bypassing Mongo
            return false;
        }
        boolean servicoAtivo;
        this.logger.info("DEV_TESTE: checando se o numero {} ja tem o servico ativo {} ", (Object)numero, (Object)servico);
        Query query = new Query();
        query.addCriteria((CriteriaDefinition)Criteria.where((String)"numero").is((Object)numero).and("servico").is((Object)servico));
        this.logger.info("DEV_TESTE: QUERY {}", (Object)query.toString());
        Map compra_numero_servico = (Map)this.mongoTemplate.findOne(query, Map.class, "");
        Gson gson = new Gson();
        boolean bl = servicoAtivo = compra_numero_servico != null;
        if (servicoAtivo) {
            this.logger.info("DEV_TESTE: QUERY RESULT", (Object)gson.toJson((Object)compra_numero_servico));
        }
        if (!servicoAtivo) {
            this.logger.info("DEV_TESTE: {} NAO  tem o servico ativo {} . posso usar", (Object)numero, (Object)servico);
            return false;
        }
        if (servicoAtivo && ((Map)compra_numero_servico.get("api_key")).equals(api_key)) {
            this.logger.info("DEV_TESTE: {} NAO  tem o servico ativo {} . nao poderia usar, mas foi usado pela minha api {}, pode retry", new Object[]{numero, servico, api_key});
            return true;
        }
        this.logger.info("DEV_TESTE:O numero {} ja  tem o servico ativo {} . NAO USAR", (Object)numero, (Object)servico);
        return true;
    }

    public void bloqueiaNumeroServico(String api_key, String codigo, String numero, String servico) {
        if (isBypassEnabled()) {
            // No-op when bypass is enabled
            return;
        }
        HashMap<String, Object> new_config = new HashMap<String, Object>();
        new_config.put("codigo", codigo);
        new_config.put("api_key", api_key);
        new_config.put("numero", numero);
        new_config.put("servico", servico);
        new_config.put("data", Instant.now());
        this.logger.info("DEV_TESTE: INSERT {}", new_config);
        this.logger.info("DEV_TESTE: TRAVANDO O NUMERO {} NO SERVICO {}", (Object)numero, (Object)servico);
        this.mongoTemplate.insert(new_config, "compras_ativadas");
    }

    public List<ChipModel> numerosQueNaoForamUsadosNesseServico(List<ChipModel> numeroDisponivelList, String servico, int limite) {
        if (isBypassEnabled()) {
            // Return input list unchanged when bypassing Mongo filters
            return numeroDisponivelList;
        }
        ArrayList<String> numeroDisponiveArray = new ArrayList<String>();
        if (numeroDisponivelList.size() > limite) {
            Collections.shuffle(numeroDisponivelList);
            numeroDisponivelList = numeroDisponivelList.subList(0, limite);
        }
        for (int i = 0; i < numeroDisponivelList.size(); ++i) {
            numeroDisponiveArray.add(numeroDisponivelList.get(i).getNumber());
        }
        Query query = new Query();
        Criteria criterio = Criteria.where((String)"numero").in(numeroDisponiveArray).and("servico").is((Object)servico);
        query.addCriteria((CriteriaDefinition)criterio);
        this.logger.info("DEV_TESTE:numerosQueNaoForamUsadosNesseServico: QUERY {}", (Object)query);
        List<Map> numeros_comprados = this.mongoTemplate.find(query, Map.class, "compras_ativadas");
        this.logger.info("DEV_TESTE:numerosQueNaoForamUsadosNesseServico: QUERY RESULT NUMEROS JA COMPRADOS NESSE SERVICO {}", (Object)query);
        for (Map compra : numeros_comprados) {
            numeroDisponivelList.removeIf(value -> value.getNumber().equals(compra.get("numero")));
        }
        this.logger.info("DEV_TESTE:NUMEROS QUE TENHO DISPONIVEL APOS REMOVER OS QUE TEM O SERVICO{}", numeroDisponivelList);
        return numeroDisponivelList;
    }

    public void insert(Map new_config) {
        if (isBypassEnabled()) {
            // No-op during bypass
            return;
        }
        this.mongoTemplate.insert((Object)new_config, "numbers_to_callback");
    }
}
