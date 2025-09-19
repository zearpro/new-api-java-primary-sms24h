/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.redis.core.RedisTemplate
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String set(String key1, String key2, String value, Duration duration) {
        String key = key1 + ":" + key2;
        this.redisTemplate.opsForValue().set(key, value, duration);
        return key;
    }

    public String get(String key1, String key2) {
        String key = key1 + ":" + key2;
        Object value = this.redisTemplate.opsForValue().get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    public Long incrementCounter(String key) {
        return this.redisTemplate.opsForValue().increment(key);
    }

    public void expire(String key, Duration duration) {
        this.redisTemplate.expire(key, duration);
    }

    public void registraServico(String numero, String servico, String ativacao) {
        String key = "SERVICOS_" + numero;
        this.redisTemplate.opsForHash().put(key, servico, ativacao);
    }

    public void servicosNoNumero(String numero) {
        String key = "SERVICOS_" + numero;
        this.redisTemplate.opsForHash().keys(key);
    }

    public void hasServico(String numero, String servico) {
        String key = "SERVICOS_" + numero;
        this.redisTemplate.opsForHash().hasKey(key, servico);
    }

    public void deleteServico(String numero, String servico) {
        String key = "SERVICOS_" + numero;
        this.redisTemplate.opsForHash().delete(key, servico);
    }

    public boolean retry(long id) {
        String key = "RETRY_" + String.valueOf(id);
        if (this.redisTemplate.opsForValue().get(key) == null) {
            this.redisTemplate.opsForValue().set(key, 1, 1L, TimeUnit.MINUTES);
            return true;
        }
        return false;
    }
}
