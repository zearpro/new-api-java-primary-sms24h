package br.com.store24h.store24h.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersistentTablesSyncService {

    private static final Logger logger = LoggerFactory.getLogger(PersistentTablesSyncService.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ZSetOperations<String, Object> zset() {
        return redisTemplate.opsForZSet();
    }

    // Redis keys
    private static final String CNC_MAX_ID_KEY = "chip_number_control:max_id";
    private static final String CNC_ZINDEX = "chip_number_control:index";
    private static final String ALIAS_MAX_CREATED_KEY = "chip_number_control_alias_service:max_created";
    private static final String ALIAS_ZINDEX = "chip_number_control_alias_service:index";

    // Generic helpers
    private Long getLongValue(String key, Long defaultVal) {
        try {
            Object v = redisTemplate.opsForValue().get(key);
            if (v == null) return defaultVal;
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private void setLongValue(String key, Long val) {
        redisTemplate.opsForValue().set(key, String.valueOf(val));
    }

    private void cacheCncRow(Map<String, Object> row) {
        Long id = ((Number) row.get("id")).longValue();
        String hkey = "chip_number_control:" + id;
        redisTemplate.opsForHash().putAll(hkey, row);
        zset().add(CNC_ZINDEX, hkey, id.doubleValue());
    }

    private void cacheAliasRow(Map<String, Object> row) {
        // Synthesize a deterministic key from created timestamp only
        Timestamp created = (Timestamp) row.get("created");
        long createdMs = created != null ? created.getTime() : Instant.now().toEpochMilli();
        String hkey = "chip_number_control_alias_service:" + createdMs;
        redisTemplate.opsForHash().putAll(hkey, row);
        zset().add(ALIAS_ZINDEX, hkey, (double) createdMs);
    }

    // Incremental by id (every 2 min)
    public void syncCncIncremental(int pageSize) {
        long lastId = getLongValue(CNC_MAX_ID_KEY, 0L);
        String sql = "SELECT id, chip_number FROM chip_number_control WHERE id > :lastId ORDER BY id ASC LIMIT :limit";
        Query q = entityManager.createNativeQuery(sql)
                .setParameter("lastId", lastId)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Object[]> raw = q.getResultList();
        if (raw.isEmpty()) return;
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Object[] arr : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) arr[0]).longValue());
            m.put("chip_number", arr[1]);
            rows.add(m);
        }
        long max = lastId;
        for (Map<String, Object> r : rows) {
            cacheCncRow(r);
            long id = ((Number) r.get("id")).longValue();
            if (id > max) max = id;
        }
        setLongValue(CNC_MAX_ID_KEY, max);
        logger.info("CNC incremental synced {} rows. max_id={}", rows.size(), max);
    }

    // Reconcile tail (every 15 min)
    public void syncCncReconcileByTail(long tailCount, int pageSize) {
        long max = getLongValue(CNC_MAX_ID_KEY, 0L);
        long startId = Math.max(0, max - tailCount);
        String sql = "SELECT id, chip_number FROM chip_number_control WHERE id > :startId ORDER BY id ASC LIMIT :limit";
        Query q = entityManager.createNativeQuery(sql)
                .setParameter("startId", startId)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Object[]> raw = q.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Object[] arr : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) arr[0]).longValue());
            m.put("chip_number", arr[1]);
            rows.add(m);
        }
        for (Map<String, Object> r : rows) {
            cacheCncRow(r);
        }
        logger.info("CNC reconcile synced {} rows from tail window starting at {}", rows.size(), startId);
    }

    // Incremental by created (every 2 min)
    public void syncAliasIncremental(int pageSize) {
        long lastCreated = getLongValue(ALIAS_MAX_CREATED_KEY, 0L);
        String sql = "SELECT created FROM chip_number_control_alias_service WHERE created > FROM_UNIXTIME(:lastMs/1000) ORDER BY created ASC LIMIT :limit";
        Query q = entityManager.createNativeQuery(sql)
                .setParameter("lastMs", lastCreated)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Object[]> raw = q.getResultList();
        if (raw.isEmpty()) return;
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Object[] arr : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("created", arr[0]);
            rows.add(m);
        }
        long maxCreated = lastCreated;
        for (Map<String, Object> r : rows) {
            cacheAliasRow(r);
            Timestamp ts = (Timestamp) r.get("created");
            if (ts != null && ts.getTime() > maxCreated) maxCreated = ts.getTime();
        }
        setLongValue(ALIAS_MAX_CREATED_KEY, maxCreated);
        logger.info("Alias incremental synced {} rows. max_created_ms={}", rows.size(), maxCreated);
    }

    // Reconcile recent time window (every 15 min)
    public void syncAliasReconcileRecentHours(int recentHours, int pageSize) {
        String sql = "SELECT created FROM chip_number_control_alias_service WHERE created >= NOW() - INTERVAL :hrs HOUR ORDER BY created ASC LIMIT :limit";
        Query q = entityManager.createNativeQuery(sql)
                .setParameter("hrs", recentHours)
                .setParameter("limit", pageSize);
        @SuppressWarnings("unchecked")
        List<Object[]> raw = q.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>(raw.size());
        for (Object[] arr : raw) {
            Map<String, Object> m = new HashMap<>();
            m.put("created", arr[0]);
            rows.add(m);
        }
        for (Map<String, Object> r : rows) {
            cacheAliasRow(r);
        }
        logger.info("Alias reconcile synced {} rows from last {} hours", rows.size(), recentHours);
    }
}


