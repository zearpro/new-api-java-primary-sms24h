/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.cache.annotation.CacheEvict
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.web.PageableDefault
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.CrossOrigin
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub;

import br.com.store24h.store24h.AsyncServiceCheckSmsReceived;
import br.com.store24h.store24h.Requisicoes.ParamActivity;
import br.com.store24h.store24h.Requisicoes.ParamDate;
import br.com.store24h.store24h.Requisicoes.RequisicaoUpdateService;
import br.com.store24h.store24h.Requisicoes.services.ParamEditPriceSevice;
import br.com.store24h.store24h.dto.ErrorResponseDto;
import br.com.store24h.store24h.dto.ServicoDtoJrx;
import br.com.store24h.store24h.model.CompraServiso;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.SvsService;
import br.com.store24h.store24h.services.UserService;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins={"*"}, allowedHeaders={"*"})
@RequestMapping(value={"/stubs/handler_api/apiServicos"})
public class ServicoApi {
    @Autowired
    private ServicosRepository servicosRepository;
    @Autowired
    private AsyncServiceCheckSmsReceived asyncService;
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    @Autowired
    private UserService userService;
    private static final Logger logger = LogManager.getLogger(ServicoApi.class);
    @Autowired
    private SvsService svsService;

    @GetMapping(value={"/activity"})
    public ResponseEntity<Object> getAllServicesActivity() {
        try {
            long startTime = System.nanoTime();
            List<Servico> servicoPage = this.asyncService.findByActivityCached(true, Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"name"}));
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            double durationSeconds = (double)durationNanos / 1.0E9;
            logger.info("[{}]Tempo gasto para carregar os servicos", (Object)durationSeconds);
            return ResponseEntity.ok(servicoPage);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto());
        }
    }

    @GetMapping(value={"/get/all/services"})
    @CrossOrigin(origins={"*"})
    public ResponseEntity<Object> getAllServicesX() {
        try {
            Sort sort = Sort.by((String[])new String[]{"name"});
            List servicoPage = this.servicosRepository.findAll(sort);
            return ResponseEntity.ok((Object)servicoPage);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("sua senha \u00e9 imcomp\u00e1tivel!"));
        }
    }

    @PostMapping(value={"/edit/price/{id}"})
    public ResponseEntity<Object> editPrice(@PathVariable Long id, @RequestBody ParamEditPriceSevice paramEditPrice) {
        try {
            this.svsService.editPriceService(id, paramEditPrice.getNewPrice());
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto());
        }
    }

    @PostMapping(value={"/setActivity/services"})
    public ResponseEntity<Object> setActivityServices(@RequestBody ParamActivity paramActivity) {
        try {
            this.svsService.editActivityServices(paramActivity.getAliasServices());
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value={"/getService/{id}"})
    public ResponseEntity<Object> getServiceJunior(@PathVariable Long id) {
        try {
            Optional servicoOptional = this.servicosRepository.findById(id);
            if (!servicoOptional.isPresent()) {
                return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Servi\u00e7o n\u00e3o encontrado!"));
            }
            Servico serv = (Servico)servicoOptional.get();
            return ResponseEntity.ok((Object)new ServicoDtoJrx(serv));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops! algo deu errado."));
        }
    }

    @CacheEvict(value={"services"})
    @PostMapping(value={"/editService/{id}"})
    public ResponseEntity<Object> editService(@PathVariable Long id, @RequestBody RequisicaoUpdateService requisicaoUpdateService) {
        try {
            Optional servicoOptional = this.servicosRepository.findById(id);
            if (!servicoOptional.isPresent()) {
                return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Servi\u00e7o n\u00e3o encontrado!"));
            }
            Servico serv = (Servico)servicoOptional.get();
            serv.setPrice(requisicaoUpdateService.getPrice());
            this.servicosRepository.save(serv);
            return ResponseEntity.ok((Object)new ServicoDtoJrx(serv));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops! algo deu errado."));
        }
    }

    @DeleteMapping(value={"/deleteService/{id}"})
    public ResponseEntity<Object> deleteService(@PathVariable Long id) {
        try {
            String response = this.svsService.deleteService(id);
            if (response == null) {
                return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Servi\u00e7o n\u00e3o encontrado!"));
            }
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops! algo deu errado."));
        }
    }

    @GetMapping(value={"/getComprasFeitas"})
    public ResponseEntity<Object> comprasFeitasHub(@PageableDefault(sort={"id"}, direction=Sort.Direction.DESC, page=0, size=10) Pageable pageable, Authentication authentication) {
        User user = this.userService.userLogado(authentication);
        if (user == null) {
            return ResponseEntity.status((int)403).body((Object)"nao logado");
        }
        Page<CompraServiso> compraServisoPage = this.buyServiceRepository.findByIdUser(user.getId(), pageable);
        return ResponseEntity.ok(compraServisoPage);
    }

    @PostMapping(value={"/getComprasFeitas/hub/filter"})
    public ResponseEntity<Object> comprasFeitasFilter(@RequestBody ParamDate paramDate, @PageableDefault(sort={"id"}, direction=Sort.Direction.DESC, page=0, size=10) Pageable pageable, Authentication authentication) {
        User user = this.userService.userLogado(authentication);
        LocalDateTime dateInitial = this.auxDate(paramDate.getDateInitial());
        LocalDateTime datefinal = this.auxDate(paramDate.getDateFinal());
        Page<CompraServiso> compraServisoPage = this.buyServiceRepository.findByLocalDateTimeBetweenAndIdUser(dateInitial, datefinal, user.getId(), pageable);
        return ResponseEntity.ok(compraServisoPage);
    }

    private LocalDateTime auxDate(String date) {
        int year = Integer.parseInt(date.substring(11, 15));
        String monthStrig = date.substring(4, 7).toUpperCase();
        monthStrig = monthStrig.equals("JAN") ? "JANUARY" : (monthStrig.equals("FEB") ? "FEBRUARY" : (monthStrig.equals("MAR") ? "MARCH" : (monthStrig.equals("APR") ? "APRIL" : (monthStrig.equals("MAY") ? "MAY" : (monthStrig.equals("JUN") ? "JUNE" : (monthStrig.equals("JUL") ? "JULY" : (monthStrig.equals("AUG") ? "AUGUST" : (monthStrig.equals("SEP") ? "SEPTEMBER" : (monthStrig.equals("OCT") ? "OCTOBER" : (monthStrig.equals("NOV") ? "NOVEMBER" : "DECEMBER"))))))))));
        Month month = Month.valueOf(monthStrig);
        int day = Integer.parseInt(date.substring(8, 10));
        int hours = Integer.parseInt(date.substring(16, 18));
        int minutes = Integer.parseInt(date.substring(19, 21));
        LocalDateTime dateOk = LocalDateTime.of(year, month, day, hours, minutes);
        return dateOk;
    }
}
