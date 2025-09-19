/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.SvsService;
import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {
    @Autowired
    private UserDbRepository userDbRepository;
    Logger logger = LoggerFactory.getLogger(CompraService.class);
    private final ExecutorService executorService0 = new ThreadPoolExecutor(20, 60, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    @Autowired
    private SvsService svsService;

    public String virifyCredit(User user, Servico servicoDb) {
        if (user.getCredito().compareTo(servicoDb.getPrice()) >= 0) {
            return "true";
        }
        return "false";
    }

    @Transactional
    public boolean subtractAndSave(User user1, Servico servico, String chipNumber, Long idActivation) {
        BigDecimal subtrair = servico.getPrice();
        int updatedRows = this.userDbRepository.decreaseSaldo(user1.getApiKey(), subtrair);
        if (System.getenv("SHOW_SALDO_LOG_SUBTRACT") != null) {
            User user = this.userDbRepository.findByApiKey(user1.getApiKey()).get();
            double saldoAnterior = user.getCredito().doubleValue() + subtrair.doubleValue();
            double novoSaldo = user.getCredito().doubleValue();
            this.logger.info("{} saldo anterior:{} reduzir:{} novo saldo:{}", new Object[]{user1.getEmail(), saldoAnterior, subtrair, novoSaldo});
        }
        this.executorService0.submit(() -> this.svsService.saveRegisterBuy(idActivation, servico, chipNumber, user1.getId()));
        return updatedRows > 0;
    }

    @Transactional
    public void devolution(String apiKey, BigDecimal price) {
        try {
            this.userDbRepository.increaseSaldo(apiKey, price);
            User user = this.userDbRepository.findByApiKey(apiKey).get();
            if (System.getenv("SHOW_SALDO_LOG_DEVOLUTION") != null) {
                BigDecimal saldoAnterior = user.getCredito().subtract(price);
                BigDecimal novoSaldo = user.getCredito();
                this.logger.info("{} saldo anterior:{} devolver:{} novo saldo:{}", new Object[]{user.getEmail(), saldoAnterior, price, novoSaldo});
            }
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC: {} falha ao devolver saldo STACK: {}", (Object)apiKey, (Object)Utils.getSingleLineStackTrace(e));
        }
    }
}
