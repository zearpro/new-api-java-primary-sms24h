/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Bean
 *  org.springframework.data.domain.Sort
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.MongoService;
import br.com.store24h.store24h.RabbitMQSender;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.ChipNumberControl;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.SmsModel;
import br.com.store24h.store24h.model.StatusChipModel;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.ChipRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.SmsRepository;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.services.SvsService;
import br.com.store24h.store24h.services.core.ActivationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class AsyncServiceCheckSmsReceived {
    @Autowired
    private SmsRepository smsRepository;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private ServicosRepository servicosRepository;
    private static final Logger logger = LogManager.getLogger(AsyncServiceCheckSmsReceived.class);
    @Autowired
    private ChipNumberControlRepository controlRepository;
    @Autowired
    private SvsService svsService;
    @Autowired
    private RabbitMQSender rabbitMQSender;
    @Autowired
    private MongoService mongoService;
    @Autowired
    private ChipRepository chipRepository;
    @Autowired
    private ChipNumberControlService chipNumberControlService;

    @Bean(name={"rulesThreadExecutor"})
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(24);
        executor.setMaxPoolSize(1128);
        executor.setQueueCapacity(11500);
        executor.setThreadNamePrefix("Rules-");
        executor.initialize();
        return executor;
    }

    public List<Servico> findByActivityCached(boolean isActive, Sort sort) {
        return this.servicosRepository.findByActivity(isActive, sort).join();
    }

    @Async
    public CompletableFuture<ChipModel> addQtdInService(List<Servico> servicoList, ChipModel chipModel) {
        try {
            Optional<ChipNumberControl> chipNumberControlOptional = this.controlRepository.findByChipNumber(chipModel.getNumber());
            if (chipNumberControlOptional.isPresent()) {
                ChipNumberControl chipNumberControl = chipNumberControlOptional.get();
                List<String> chipNumberServicosList = chipNumberControl.getAliasService();
                ArrayList<Servico> servicesModified = new ArrayList<Servico>();
                servicoList.forEach(servico -> {
                    int index = chipNumberServicosList.indexOf(servico.getAlias());
                    if (index == -1) {
                        servicesModified.add((Servico)servico);
                    }
                });
                this.svsService.addQuantityAllService(servicesModified);
            } else {
                this.chipNumberControlService.newChipNumberControl(chipModel.getNumber());
                this.svsService.addQuantityAllService(servicoList);
            }
            chipModel.setChecked(true);
            chipModel.setAtivo(true);
            chipModel.setStatus(StatusChipModel.ACTIVITY.getStatus());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(chipModel);
    }

    @Async
    public CompletableFuture<ChipModel> checkIfPresentNumberControl(ChipModel chipModel) {
        Optional<ChipNumberControl> chipNumberControlOptional = this.controlRepository.findByChipNumber(chipModel.getNumber());
        if (!chipNumberControlOptional.isPresent()) {
            this.chipNumberControlService.newChipNumberControl(chipModel.getNumber());
        }
        logger.info("checkIfPresentNumberControl done {} ", (Object)chipModel.getNumber());
        return CompletableFuture.completedFuture(chipModel);
    }

    @Async
    public CompletableFuture<Activation> processActivationTask(Activation activation) {
        boolean isSmshub;
        boolean bl = isSmshub = System.getenv("ATIVACOES_SMSHUB") != null;
        if (activation.getApiKey().equals("2bce230ccb852582f693e803def487aa") && !isSmshub) {
            logger.info("ativacao smshub e aqui nao e smshub : {}", (Object)activation.getApiKey());
            return CompletableFuture.completedFuture(activation);
        }
        if (!activation.getApiKey().equals("2bce230ccb852582f693e803def487aa") && isSmshub) {
            logger.info("ativacao nao e smshub e aqui tem que ser : {}", (Object)activation.getApiKey());
            return CompletableFuture.completedFuture(activation);
        }
        logger.info("posso procesasr ativacao : {}", (Object)(isSmshub ? "eh smshub " : "nao e smshub"));
        logger.info("----");
        logger.info("id: {} number:{} BUSCA SMS MODEL", (Object)String.valueOf(activation.getId()), (Object)activation.getChipNumber());
        Optional smsModelOptional = this.smsRepository.findByChipnumberAndIdActivationOrderByIdDesc(activation.getChipNumber(), activation.getId()).stream().findFirst();
        logger.info("id: {} number:{} [FIM] BUSCA SMS MODEL", (Object)String.valueOf(activation.getId()), (Object)activation.getChipNumber());
        if (smsModelOptional.isPresent()) {
            SmsModel smsModel = (SmsModel)smsModelOptional.get();
            if (smsModel.getPretry() == 0) {
                return CompletableFuture.completedFuture(activation);
            }
            this.activationService.saveSms(activation, smsModel.getMsg(), smsModel.getDate());
            logger.info("id: {} number:{} codigo:{} msg:{}", (Object)String.valueOf(activation.getId()), (Object)activation.getChipNumber(), (Object)smsModel.getMsg(), (Object)smsModel.getMsg_full());
            this.rabbitMQSender.messageReceived(activation.getServiceNumber(), activation.getChipNumber(), activation.getAliasService(), activation.getId(), smsModel.getMsg_full());
            this.mongoService.bloqueiaNumeroServico(activation.getApiKey(), smsModel.getMsg(), activation.getChipNumber(), activation.getAliasService());
        } else {
            logger.info("id: {} number:{} codigo:{}", (Object)String.valueOf(activation.getId()), (Object)activation.getChipNumber(), (Object)"ainda sem sms");
        }
        return CompletableFuture.completedFuture(activation);
    }
}
