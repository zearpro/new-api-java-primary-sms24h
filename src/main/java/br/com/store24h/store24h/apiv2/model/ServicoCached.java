/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.json.JSONObject
 */
package br.com.store24h.store24h.apiv2.model;

import br.com.store24h.store24h.model.Servico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

public class ServicoCached {
    static ObjectMapper mapper = new ObjectMapper();

    public static JSONObject create(Servico s) throws JsonProcessingException {
        String jsonString = mapper.writeValueAsString((Object)s);
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject;
    }
}
