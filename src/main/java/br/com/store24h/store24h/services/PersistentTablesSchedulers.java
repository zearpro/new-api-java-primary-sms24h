package br.com.store24h.store24h.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PersistentTablesSchedulers {

    private static final Logger logger = LoggerFactory.getLogger(PersistentTablesSchedulers.class);

    @Autowired
    private PersistentTablesSyncService syncService;

    @Value("${cache.warming.cnc.incremental.rate:120000}")
    private long cncIncrementalRate;

    @Value("${cache.warming.cnc.reconcile.rate:900000}")
    private long cncReconcileRate;

    @Value("${cache.warming.alias.incremental.rate:120000}")
    private long aliasIncrementalRate;

    @Value("${cache.warming.alias.reconcile.rate:900000}")
    private long aliasReconcileRate;

    // v_operadoras schedule is already managed by OperatorsCacheService warmup (5 min via properties)

    // Incremental: chip_number_control every 2 minutes
    @Scheduled(fixedRateString = "${cache.warming.cnc.incremental.rate:120000}")
    public void runCncIncremental() {
        try {
            syncService.syncCncIncremental(10000);
        } catch (Exception e) {
            logger.error("CNC incremental scheduler error", e);
        }
    }

    // Reconcile: chip_number_control every 15 minutes (tail window heal)
    @Scheduled(fixedRateString = "${cache.warming.cnc.reconcile.rate:900000}")
    public void runCncReconcile() {
        try {
            syncService.syncCncReconcileByTail(500000, 20000);
        } catch (Exception e) {
            logger.error("CNC reconcile scheduler error", e);
        }
    }

    // Incremental: alias service every 2 minutes
    @Scheduled(fixedRateString = "${cache.warming.alias.incremental.rate:120000}")
    public void runAliasIncremental() {
        try {
            syncService.syncAliasIncremental(10000);
        } catch (Exception e) {
            logger.error("Alias incremental scheduler error", e);
        }
    }

    // Reconcile: alias service every 15 minutes (recent hours sweep)
    @Scheduled(fixedRateString = "${cache.warming.alias.reconcile.rate:900000}")
    public void runAliasReconcile() {
        try {
            syncService.syncAliasReconcileRecentHours(48, 20000);
        } catch (Exception e) {
            logger.error("Alias reconcile scheduler error", e);
        }
    }
}


