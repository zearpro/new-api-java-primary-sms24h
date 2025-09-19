/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.client.ClientHttpRequestFactory
 *  org.springframework.http.client.SimpleClientHttpRequestFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.client.RestTemplate
 */
package br.com.store24h.store24h.services.core;

import br.com.store24h.store24h.MongoService;
import br.com.store24h.store24h.RabbitMQSender;
import br.com.store24h.store24h.RedisService;
import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.TimeZone;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ActivationRepository;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.SmsRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.ChipNumberControlService;
import br.com.store24h.store24h.services.CompraService;
import br.com.store24h.store24h.services.OtherService;
import br.com.store24h.store24h.services.SvsService;
import br.com.store24h.store24h.services.core.ActivationStatus;
import br.com.store24h.store24h.services.core.ServiceMapAlg;
// import br.com.store24h.store24h.task.ActivationTask; // REMOVED: Task classes disabled for production
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class ActivationService {
    private AtomicInteger counter = new AtomicInteger(0);
    @Autowired
    private ActivationRepository activationRepository;
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private CompraService compraService;
    @Autowired
    private RabbitMQSender rabbitMQSender;
    @Autowired
    private MongoService mongoService;
    @Autowired
    private SmsRepository smsRepository;
    @Autowired
    private ServiceMapAlg serviceMapAlg;
    @Autowired
    private ChipNumberControlService controlService;
    @Autowired
    private SvsService svsService;
    @Autowired
    private OtherService otherService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    private static final Logger logger = LogManager.getLogger(ActivationService.class);
    private final ExecutorService executorService0 = new ThreadPoolExecutor(Utils.getThreadCancelamento(), Utils.getThreadCancelamento(), 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(Utils.getLimiteTotalCancelamento()), new ThreadPoolExecutor.AbortPolicy());
    private final ExecutorService executorService1 = new ThreadPoolExecutor(20, 60, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());
    private final ExecutorService executorService2 = new ThreadPoolExecutor(20, 60, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5580), new ThreadPoolExecutor.AbortPolicy());
    @Autowired
    private RedisService redisService;

    @Transactional
    public Activation newActivation(User user, Servico servico, String chipNumber, String apiKey, int version) {
        try {
            int visible = version == 1 ? 1 : 0;
            Activation activation = new Activation(servico, chipNumber, apiKey, version, visible);
            activation.setStatusBuz(ActivationStatus.AGUARDANDO_MENSAGENS);
            Long id = ((Activation)this.activationRepository.save(activation)).getId();
            this.executorService1.submit(() -> {
                boolean alterado = false;
                alterado = apiKey.equals("2bce230ccb852582f693e803def487aa") ? true : this.compraService.subtractAndSave(user, servico, chipNumber, id);
                if (!alterado) {
                    User usuarioNovo = this.userDbRepository.findFirstByApiKey(user.getApiKey()).get();
                    logger.info("[ativacao {}]{} o saldo eh menor que zero, vou cancelar.saldo: {}", (Object)activation.getId(), (Object)user.getApiKey(), (Object)usuarioNovo.getCredito());
                    this.otherService.remove(activation.getChipNumber());
                    activation.setStatusBuz(ActivationStatus.CANCELADA);
                    activation.setStatus(8);
                    this.controlService.removeService(activation.getChipNumber(), activation.getAliasService());
                    activation.setVisible(1);
                    activation.setAliasService(activation.getAliasService() + "_cancel");
                    activation.setServiceNumber("insuficient_founds");
                    this.activationRepository.save(activation);
                    if (version == 2) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            return activation;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getCancelQueue() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)this.executorService0;
        int enqueuedSize = threadPoolExecutor.getQueue().size();
        int activeThreads = threadPoolExecutor.getActiveCount();
        int poolSize = threadPoolExecutor.getPoolSize();
        logger.info("CANCEL_QUEUE enqueuedSize: {}  activeThreads:{} poolSize:{} ", (Object)enqueuedSize, (Object)activeThreads, (Object)poolSize);
        return enqueuedSize;
    }

    @Transactional
    public void cancelActivation(Long id, String apiKey) {
        logger.info("ADD_THREAD_CANCEL {}", (Object)id);
        this.executorService0.submit(() -> {
            logger.info("START_THREAD_CANCEL {}", (Object)id);
            Activation activation = this.activationRepository.findById(id).get();
            ActivationStatus lastStatus = activation.getStatusBuz();
            if (activation.getSmsStringModels().isEmpty() && lastStatus.equals((Object)ActivationStatus.CANCELADA)) {
                logger.info("AO TENTAR CANCELAR A ATIVACAO {} . ELA JA ESTAVA CANCELADA", (Object)id);
            } else if (activation.getSmsStringModels().isEmpty()) {
                activation.setStatus(8);
                this.controlService.removeService(activation.getChipNumber(), activation.getAliasService());
                activation.setAliasService(activation.getAliasService() + "_cancel");
                activation.setStatusBuz(ActivationStatus.CANCELADA);
                activation.setVisible(1);
                this.activationRepository.save(activation);
                this.saveStatusBuy(activation.getId(), 8, null);
                this.otherService.remove(activation.getChipNumber());
                this.redisService.deleteServico(activation.getChipNumber(), activation.getAliasService().split("_")[0]);
                if (!apiKey.equals("2bce230ccb852582f693e803def487aa")) {
                    this.compraService.devolution(apiKey, activation.getServicePrice());
                }
                logger.info("FINISH_THREAD_CANCEL {}", (Object)id);
            } else {
                this.conclude(id, "veio pelo cron");
            }
        });
    }

    @Transactional
    public synchronized String cancelActivation(Activation activation, String apiKey) throws InterruptedException {
        return this.cancelActivation(activation, apiKey, 1);
    }

    @Transactional
    public synchronized String cancelar(Activation activation, String apiKey) {
        String CANCEL_KEY_R = "cancel:" + String.valueOf(activation.getId());
        Long tentativa = this.redisService.incrementCounter(CANCEL_KEY_R);
        logger.info("[CANCEL_ACTIVATION] {} : TENTATIVA ATUAL: {} ", (Object)activation.getId(), (Object)tentativa);
        if (tentativa > 1L) {
            logger.info("[CANCEL_ACTIVATION] CANCEL Activation id " + String.valueOf(activation.getId()) + " REJEITA PELAS TENTATIVAS");
            this.redisService.expire(CANCEL_KEY_R, Duration.ofSeconds(30L));
            return "ACCESS_CANCEL_DUPLICATED";
        }
        this.redisService.expire(CANCEL_KEY_R, Duration.ofMinutes(10L));
        activation = this.activationRepository.findById(activation.getId()).get();
        if (activation.getStatusBuz() == ActivationStatus.RECEBIDA) {
            this.conclude(activation.getId(), "veio pela api e estava com status RECEBIDA");
            return "ACCESS_ACTIVATION";
        }
        logger.info("[CANCEL_ACTIVATION] after sleep, CANCEL Activation id " + String.valueOf(activation.getId()) + " " + activation.getStatusBuz().toString());
        if (activation.getStatusBuz() == ActivationStatus.CANCELADA) {
            logger.info("[CANCEL_ACTIVATION] CANCEL Activation id " + String.valueOf(activation.getId()) + " JA ESTAVA CANCELADO");
            return "ACCESS_CANCEL_DUPLICATED";
        }
        logger.info("[CANCEL_ACTIVATION] continue CANCEL Activation id " + String.valueOf(activation.getId()) + " ");
        String aliasService = activation.getAliasService();
        activation.setStatus(8);
        this.controlService.removeService(activation.getChipNumber(), aliasService);
        activation.setAliasService(aliasService + "_cancel");
        activation.setStatusBuz(ActivationStatus.CANCELADA);
        activation.setVisible(1);
        this.activationRepository.save(activation);
        this.saveStatusBuy(activation.getId(), 8, null);
        if (!apiKey.equals("2bce230ccb852582f693e803def487aa")) {
            this.compraService.devolution(apiKey, activation.getServicePrice());
        }
        this.otherService.remove(activation.getChipNumber());
        this.redisService.deleteServico(activation.getChipNumber(), activation.getAliasService().split("_")[0]);
        return "ACCESS_CANCEL";
    }

    @Transactional
    public synchronized String cancelActivation(Activation activation, String apiKey, int version) throws InterruptedException {
        if (activation.getSmsStringModels().isEmpty()) {
            if (version == 1) {
                return this.cancelar(activation, apiKey);
            }
            this.cancelar(activation, apiKey);
            return "ACCESS_CANCEL";
        }
        this.conclude(activation.getId(), "veio pela api");
        return "ACCESS_ACTIVATION";
    }

    public Activation conclude(Long id, String causa) {
        Activation activation = this.activationRepository.findById(id).get();
        if (!activation.getAliasService().contains("_finalizada")) {
            activation.setAliasService(activation.getAliasService() + "_finalizada");
            activation.setStatus(6);
            activation.setStatusBuz(ActivationStatus.FINALIZADA);
            activation.setVisible(1);
            this.saveStatusBuy(activation.getId(), 6, null);
            this.activationRepository.save(activation);
            this.otherService.remove(activation.getChipNumber());
            String msg = activation.getSmsStringModels().get(activation.getSmsStringModels().size() - 1);
            logger.info("[{}] Ativacao {} ia ser cancelada mas tem sms recebido, seta _finalizada. SMS: {}", (Object)causa, (Object)activation.getId(), (Object)msg);
            this.rabbitMQSender.messageReceived(activation.getServiceNumber(), activation.getChipNumber(), activation.getAliasService(), activation.getId(), msg);
            this.mongoService.bloqueiaNumeroServico(activation.getApiKey(), msg, activation.getChipNumber(), activation.getAliasService());
            this.redisService.registraServico(activation.getChipNumber(), activation.getAliasService().split("_")[0], String.valueOf(activation.getId()));
        } else {
            logger.info("[{}] Ativacao {} JA ESTAVA CONCLUIDA", (Object)causa, (Object)activation.getId());
        }
        return activation;
    }

    public void conclude(Activation activation) {
        if (!activation.getAliasService().contains("_finalizada")) {
            logger.info("Ativacao {} Set conclude statusbuz FINALIZADA status 6", (Object)activation.getId());
            activation.setAliasService(activation.getAliasService() + "_finalizada");
            activation.setStatus(6);
            activation.setStatusBuz(ActivationStatus.FINALIZADA);
            this.saveStatusBuy(activation.getId(), 6, null);
            this.otherService.remove(activation.getChipNumber());
            this.activationRepository.save(activation);
        } else {
            logger.info("Ativacao {} JA ESTAVA CONCLUIDA", (Object)activation.getId());
        }
    }

    public Activation saveSms(Activation a, String sms, LocalDateTime receivedDate) {
        logger.info("id: {} number:{} codigo:{} receivedDate: {} [setando hora fim]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms, (Object)receivedDate.toString());
        a.setEndTime(LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())));
        try {
            LocalDateTime endTimeBr = a.getEndTime();
            LocalDateTime receivedDateBr = receivedDate.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.of(TimeZone.BR.getZone())).toLocalDateTime();
            Duration duration = Duration.between(receivedDateBr, endTimeBr);
            long totalSeconds = duration.getSeconds();
            int hours = (int)(totalSeconds / 3600L);
            int minutes = (int)(totalSeconds % 3600L / 60L);
            int seconds = (int)(totalSeconds % 60L);
            int milliseconds = (int)(duration.toMillis() % 1000L);
            LocalTime receivedToFinishTime = LocalTime.of(hours, minutes, seconds, milliseconds * 1000000);
            logger.info("id: {} number:{} codigo:{} tempo:{} [TEMPO ENTRE O RECEBIDOMENTO E A IDENTIFICACAO]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms, (Object)receivedToFinishTime.toString());
            a.setReceivedToFinishTime(receivedToFinishTime.toString());
        }
        catch (Exception exception) {
            // empty catch block
        }
        logger.info("id: {} number:{} codigo:{} [adicionando smsstring]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms);
        a.getSmsStringModels().add(sms);
        logger.info("id: {} number:{} codigo:{} [setando status para 7]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms);
        a.setStatus(7);
        logger.info("id: {} number:{} codigo:{} [setando sms recebido]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms);
        a.setStatusBuz(ActivationStatus.RECEBIDA);
        logger.info("id: {} number:{} codigo:{} [savando statusby]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms);
        this.saveStatusBuy(a.getId(), 7, sms);
        logger.info("id: {} number:{} codigo:{} [finalizou o save, return]", (Object)String.valueOf(a.getId()), (Object)a.getChipNumber(), (Object)sms);
        return a;
    }

    public Activation saveSmsRetry(Activation a, String sms) {
        a.setEndTime(LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())));
        a.getSmsStringModels().remove(0);
        a.getSmsStringModels().add(sms);
        a.setStatus(7);
        a.setStatusBuz(ActivationStatus.RECEBIDA);
        a.setVisible(1);
        this.saveStatusBuy(a.getId(), 7, sms);
        return a;
    }

    public boolean initialStatus(Activation activation) {
        return activation.getStatus() == -1;
    }

    public void prepareRetry(Long idActivation) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(1000);
            factory.setReadTimeout(1000);
            RestTemplate restTemplate = new RestTemplate((ClientHttpRequestFactory)factory);
            String fooResourceUrl = "http://lb-back-smshub-984990312.us-east-2.elb.amazonaws.com/prepare_retry?ativacao=" + idActivation.toString();
            ResponseEntity responseEntity = restTemplate.getForEntity(fooResourceUrl, String.class, new Object[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public boolean awaitNewCode(Activation activation) {
        logger.info("Ativacao {} VAI PEDIR RETRY", (Object)activation.getId());
        List<Integer> numberInvalids = Arrays.asList(-1, 6, 8);
        if (numberInvalids.contains(activation.getStatus())) {
            return false;
        }
        activation.setStatus(3);
        activation.setStatusBuz(ActivationStatus.AGUARDANDO_MENSAGENS);
        this.activationRepository.save(activation);
        this.saveStatusBuy(activation.getId(), 3, null);
        this.prepareRetry(activation.getId());
        return true;
    }

    public List<Activation> getActivationsValidsVisible(String apiKey) {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())).minusMinutes(20L);
        List<Integer> statusList = Arrays.asList(8, 6);
        List<Activation> activationList = this.activationRepository.findByApiKeyAndInitialTimeAfterAndStatusNotInAndVisible(apiKey, date, statusList, 1);
        return activationList;
    }

    public List<Activation> getActivationsValids(String apiKey) {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())).minusMinutes(20L);
        List<Integer> statusList = Arrays.asList(8, 6);
        List<Activation> activationList = this.activationRepository.findByApiKeyAndInitialTimeAfterAndStatusNotIn(apiKey, date, statusList);
        return activationList;
    }

    public List<Activation> getAllActivationsApiKey(String apiKey) {
        List<Activation> activationList = this.activationRepository.findByApiKeyAndVisible(apiKey, 1);
        return activationList;
    }

    private void saveStatusBuy(Long idActivation, int status, String sms) {
        if (sms != null) {
            this.updatebuyServiceRepositoryByIdActivation(idActivation, sms, status, 1);
        } else {
            logger.info("UPDATE CompraServiso c SET  c.status = {}, c.visible = {} WHERE c.idActivation = {}", (Object)status, (Object)1, (Object)idActivation);
            this.updatebuyServiceRepositoryByIdActivation(idActivation, status, 1);
            logger.info("done  CompraServiso c.status = {}, c.visible = {}  where c.idActivation = {}", (Object)status, (Object)1, (Object)idActivation);
        }
    }

    @Transactional
    public void updatebuyServiceRepositoryByIdActivation(Long idActivation, String sms, int status, int visible) {
        logger.info("RUN ------- SELECT c.id FROM registro_de_compras c WHERE c.id_activation = {} ORDER BY c.id DESC LIMIT 1", (Object)idActivation);
        Optional<Long> latestId = Optional.ofNullable(this.cacheService.getCompraServicoByActivation(idActivation.toString()));
        if (latestId.isPresent()) {
            logger.info("CACHE:cacheCompraServicoByActivation T1: {} found: {}", (Object)idActivation, (Object)latestId.get());
        } else {
            logger.info("CACHE:cacheCompraServicoByActivation T1: {} not found", (Object)idActivation);
        }
        if (!latestId.isPresent()) {
            latestId = this.buyServiceRepository.findLatestIdByIdActivation(idActivation);
            if (latestId.isPresent()) {
                logger.info("CACHE:cacheCompraServicoByActivation T2: {} found: {}", (Object)idActivation, (Object)latestId.get());
                this.cacheService.setCompraServicoByActivation(idActivation.toString(), latestId.get().toString(), Duration.ofHours(24L));
            } else {
                logger.info("CACHE:cacheCompraServicoByActivation T2: {} not found", (Object)idActivation);
            }
        }
        logger.info("DONE ------- SELECT c.id FROM registro_de_compras c WHERE c.id_activation = {} ORDER BY c.id DESC LIMIT 1", (Object)idActivation);
        latestId.ifPresent(id -> this.updateBuyServiceById((Long)id, sms, status, visible));
    }

    @Transactional
    public void updatebuyServiceRepositoryByIdActivation(Long idActivation, int status, int visible) {
        logger.info("RUN ------- SELECT c.id FROM registro_de_compras c WHERE c.id_activation = {} ORDER BY c.id DESC LIMIT 1", (Object)idActivation);
        Optional<Long> latestId = Optional.ofNullable(this.cacheService.getCompraServicoByActivation(idActivation.toString()));
        if (latestId.isPresent()) {
            logger.info("CACHE:cacheCompraServicoByActivation T1: {} found: {}", (Object)idActivation, (Object)latestId.get());
        } else {
            logger.info("CACHE:cacheCompraServicoByActivation T1: {} not found", (Object)idActivation);
        }
        if (!latestId.isPresent()) {
            latestId = this.buyServiceRepository.findLatestIdByIdActivation(idActivation);
            if (latestId.isPresent()) {
                logger.info("CACHE:cacheCompraServicoByActivation T2: {} found: {}", (Object)idActivation, (Object)latestId.get());
                this.cacheService.setCompraServicoByActivation(idActivation.toString(), latestId.get().toString(), Duration.ofHours(24L));
            } else {
                logger.info("CACHE:cacheCompraServicoByActivation T2: {} not found", (Object)idActivation);
            }
        }
        logger.info("DONE ------- SELECT c.id FROM registro_de_compras c WHERE c.id_activation = {} ORDER BY c.id DESC LIMIT 1", (Object)idActivation);
        latestId.ifPresent(id -> this.updateBuyServiceById((Long)id, status, visible));
    }

    @Transactional
    private void updateBuyServiceById(Long id, int status, int visible) {
        logger.info("RUN ------- UPDATE CompraServiso c SET   c.status = {}, c.visible = {} WHERE c.id = {}", (Object)status, (Object)1, (Object)id);
        this.buyServiceRepository.updateById(id, status, visible);
        logger.info("DONE ------ UPDATE CompraServiso c SET   c.status = {}, c.visible = {} WHERE c.id = {}", (Object)status, (Object)1, (Object)id);
    }

    @Transactional
    private void updateBuyServiceById(Long id, String sms, int status, int visible) {
        logger.info("RUN ------- UPDATE CompraServiso c SET c.sms = {}, c.status = {}, c.visible = {} WHERE c.id = {}", (Object)sms, (Object)status, (Object)1, (Object)id);
        this.buyServiceRepository.updateById(id, sms, status, visible);
        logger.info("DONE ------ UPDATE CompraServiso c SET c.sms = {}, c.status = {}, c.visible = {} WHERE c.id = {}", (Object)sms, (Object)status, (Object)1, (Object)id);
    }
}
