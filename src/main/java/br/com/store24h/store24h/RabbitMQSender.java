/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  org.springframework.amqp.rabbit.core.RabbitTemplate
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void messageReceived(String from, String to, String service, long activationId, String text) {
        Gson gson = new Gson();
        JsonObject element = new JsonObject();
        element.addProperty("from", from);
        element.addProperty("to", to);
        element.addProperty("service", service);
        element.addProperty("activationId", (Number)activationId);
        element.addProperty("text", text);
        String jsonString = gson.toJson((JsonElement)element);
        this.rabbitTemplate.convertAndSend("", "smshub.proxy", (Object)jsonString);
    }

    public void cancelarAtivacao(long activationId) {
        Gson gson = new Gson();
        JsonObject element = new JsonObject();
        element.addProperty("activationId", (Number)activationId);
        String jsonString = gson.toJson((JsonElement)element);
        this.rabbitTemplate.convertAndSend("", "service.cancel", (Object)jsonString);
    }

    public void devolverSaldo(long activationId, long usuarioId, float valor) {
        Gson gson = new Gson();
        JsonObject element = new JsonObject();
        element.addProperty("usuarioId", (Number)usuarioId);
        element.addProperty("to", (Number)Float.valueOf(valor));
        element.addProperty("activationId", (Number)activationId);
        String jsonString = gson.toJson((JsonElement)element);
        this.rabbitTemplate.convertAndSend("", "saldo.devolver", (Object)jsonString);
    }
}
