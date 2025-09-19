/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.apiv2.services;

import br.com.store24h.store24h.MongoService;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.User;
import java.lang.invoke.CallSite;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsReplicateService {
    static Logger logger = LoggerFactory.getLogger(SmsReplicateService.class);
    @Autowired
    private MongoService mongoService;
    @Autowired
    private CacheService cacheService;

    public JSONObject sendFakeRequest(User user) {
        Map<String, String> keysMap = Map.of("id", Utils.generateRandomNumberAsString(6), "api_key", user.getApiKey(), "number", "55" + Utils.generateRandomNumberAsString(11), "from", "55" + Utils.generateRandomNumberAsString(11), "text", "Seu codigo e " + Utils.generateRandomNumberAsString(3) + "-" + Utils.generateRandomNumberAsString(3));
        Map<String, Object> objMap = this.cacheService.findApiCallback(user.getCallbackUrlId());
        objMap.replaceAll((key, value) -> {
            for (Map.Entry<String, String> entry : keysMap.entrySet()) {
                if (!(value instanceof String)) continue;
                value = value.toString().replace("$" + (String)entry.getKey(), (CharSequence)entry.getValue());
            }
            return value;
        });
        try {
            return Utils.sendHttpRequest(objMap.get("url").toString(), objMap.get("method").toString(), new HashMap<String, String>(), objMap.get("body_template").toString());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addNumberToReplicateInApi(Activation activation, User user) {
        Map<String, String> keysMap = Map.of("id", String.valueOf(activation.getId()), "api_key", user.getApiKey(), "number", activation.getChipNumber());
        HashMap<String, Object> new_config = new HashMap<String, Object>();
        Map<String, Object> objMap = this.cacheService.findApiCallback(user.getCallbackUrlId());
        objMap.replaceAll((key, value) -> {
            for (Map.Entry<String, String> entry : keysMap.entrySet()) {
                if (!(value instanceof String)) continue;
                value = value.toString().replace("$" + (String)entry.getKey(), (CharSequence)entry.getValue());
            }
            return value;
        });
        new_config.put("activation_id", activation.getId());
        new_config.put("number", activation.getChipNumber());
        new_config.put("callback_id", user.getCallbackUrlId());
        new_config.put("callback_obj", objMap);
        new_config.put("createdAt", Instant.now());
        logger.info("addNumberToReplicateInApi: INSERT {}", new_config);
        this.mongoService.insert(new_config);
    }
}
