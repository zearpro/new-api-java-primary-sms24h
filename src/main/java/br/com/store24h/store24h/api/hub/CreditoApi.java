/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.Valid
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.web.PageableDefault
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.Requisicoes.RequisicaoCredito;
import br.com.store24h.store24h.dto.CreditoDTO;
import br.com.store24h.store24h.dto.ErrorResponseDto;
import br.com.store24h.store24h.model.ComprasCredito;
import br.com.store24h.store24h.model.TimeZone;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ComprasCreditoRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class CreditoApi {
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private ComprasCreditoRepository comprasCreditoRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private Funcionalidades funcionalidades;

    @PostMapping(value={"/comprarCredito"})
    public ResponseEntity<Object> comprarCredito(@RequestBody @Valid RequisicaoCredito requisicaoCredito, Authentication authentication) {
        try {
            BigDecimal credito = requisicaoCredito.getValue();
            this.funcionalidades.addCredito(this.userDbRepository, credito, authentication);
            ComprasCredito comprasCredito = new ComprasCredito(LocalDateTime.now(ZoneId.of(TimeZone.BR.getZone())), credito);
            this.comprasCreditoRepository.save(comprasCredito);
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops, algo deu errado!"));
        }
    }

    @GetMapping(value={"/getCredito"})
    public ResponseEntity<Object> getCredito(Authentication authentication) {
        try {
            User user = this.userService.userLogado(authentication);
            return ResponseEntity.ok().body((Object)new CreditoDTO(user.getCredito()));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops, algo deu errado!"));
        }
    }

    @GetMapping(value={"/getTableCredito"})
    public ResponseEntity<Object> getTableCredito(@PageableDefault(sort={"localDateTime"}, direction=Sort.Direction.DESC, page=0, size=2) Pageable pageable) {
        try {
            Page comprasCreditoPage = this.comprasCreditoRepository.findAll(pageable);
            return ResponseEntity.ok().body((Object)comprasCreditoPage);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body((Object)new ErrorResponseDto("Ops, algo deu errado!"));
        }
    }
}
