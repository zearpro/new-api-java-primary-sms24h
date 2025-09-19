/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub.user;

import br.com.store24h.store24h.dto.ActivationsDTO;
import br.com.store24h.store24h.dto.HistoryBuysDTO;
import br.com.store24h.store24h.dto.StatusDTO;
import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.model.TimeZone;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ActivationRepository;
import br.com.store24h.store24h.services.UserService;
import br.com.store24h.store24h.services.core.ActivationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api/activations"})
public class ActivationsApi {
    @Autowired
    private ActivationRepository activationRepository;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private UserService userService;

    @GetMapping
    @Transactional
    public ResponseEntity<List<ActivationsDTO>> getSmsApi(Authentication authentication) {
        try {
            User user = this.userService.userLogado(authentication);
            ArrayList<ActivationsDTO> activationsDTOList = new ArrayList<ActivationsDTO>();
            List<Activation> activationList = this.activationService.getActivationsValidsVisible(user.getApiKey());
            for (Activation a : activationList) {
                ActivationsDTO activationsDTO = new ActivationsDTO();
                LocalDateTime dataCriacao = a.getInitialTime().plusMinutes(21L);
                LocalDateTime agora = LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone()));
                Duration duracao = Duration.between(dataCriacao, agora);
                long minutosPassados = duracao.getSeconds() / 60L;
                activationsDTO.setIdUser(-1L);
                activationsDTO.setIdActivation(a.getId());
                activationsDTO.setNameService(a.getServiceName());
                activationsDTO.setAliasService(a.getAliasService());
                activationsDTO.setNumberActivation(a.getChipNumber());
                activationsDTO.setRetry(a.getStatus() == 3);
                activationsDTO.setMin(String.valueOf(Math.abs(minutosPassados)));
                if (a.getSmsStringModels().isEmpty() || a.getStatus() == 3) {
                    activationsDTO.setStatus("Envie o c\u00f3digo para o n\u00famero recebido");
                    activationsDTO.setAwaitSms(true);
                } else {
                    activationsDTO.setStatus("Confirme a exatid\u00e3o do c\u00f3digo");
                    activationsDTO.getSmsList().add(a.getSmsStringModels().get(a.getSmsStringModels().size() - 1));
                    activationsDTO.setAwaitSms(false);
                    activationsDTO.setFinalized(true);
                }
                activationsDTOList.add(activationsDTO);
            }
            List<ActivationsDTO> sortedListReverse = activationsDTOList.stream().sorted(Comparator.comparing(ActivationsDTO::getIdActivation).reversed()).toList();
            return ResponseEntity.ok(sortedListReverse);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value={"/valids"})
    public ResponseEntity<List<Activation>> activationsValids(@RequestParam String api_key) {
        List<Activation> activationList = new ArrayList<>();
        try {
            activationList = this.activationService.getActivationsValidsVisible(api_key);
            return ResponseEntity.ok(activationList);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(activationList);
        }
    }

    @PostMapping(value={"/status"})
    public ResponseEntity<List<StatusDTO>> statusActivations(@RequestParam String api_key, @RequestBody List<Long> idActivations) {
        ArrayList<StatusDTO> statusDTOList = new ArrayList<>();
        if (this.userService.isValidApiKey(api_key)) {
            List<Activation> activationList = this.activationRepository.findAllByIdInAndVisibleAndVersion(idActivations, 1, 1);
            activationList.forEach(activation -> {
                StatusDTO statusDTO = new StatusDTO(activation);
                statusDTOList.add(statusDTO);
            });
        }
        return ResponseEntity.ok(statusDTOList);
    }

    @GetMapping(value={"/history"})
    public ResponseEntity<List<HistoryBuysDTO>> historyActivations(@RequestParam String api_key) {
        ArrayList<HistoryBuysDTO> historyBuysDTOS = new ArrayList<>();
        try {
            List<Activation> activationList = this.activationService.getAllActivationsApiKey(api_key);
            activationList.forEach(a -> {
                HistoryBuysDTO historyBuysDTO = new HistoryBuysDTO(a);
                historyBuysDTOS.add(historyBuysDTO);
            });
            return ResponseEntity.ok(historyBuysDTOS);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(historyBuysDTOS);
        }
    }
}
