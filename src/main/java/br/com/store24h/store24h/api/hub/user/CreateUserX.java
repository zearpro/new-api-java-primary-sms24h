/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.Valid
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.util.UriComponentsBuilder
 */
package br.com.store24h.store24h.api.hub.user;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.Requisicoes.RequisicaoNovoUser;
import br.com.store24h.store24h.dto.ErrorCadastroDTO;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.UserDbRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class CreateUserX {
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private Funcionalidades funcionalidades;

    @PostMapping(value={"/createUser"})
    public ResponseEntity<Object> createUser(@RequestBody @Valid RequisicaoNovoUser requisicaoNovoUser, UriComponentsBuilder uriBuilder) {
        Optional<User> userOptional = this.userDbRepository.findByEmail(requisicaoNovoUser.getEmail());
        if (requisicaoNovoUser.getSenhaUser().equals(requisicaoNovoUser.getSenhaUser2()) && !userOptional.isPresent()) {
            User user = requisicaoNovoUser.toUser(this.funcionalidades);
            BigDecimal newCredito = BigDecimal.valueOf(0L);
            user.setCredito(newCredito);
            this.userDbRepository.save(user);
            URI uri = uriBuilder.path("/createUser/{id}").buildAndExpand(new Object[]{user.getId()}).toUri();
            return ResponseEntity.created((URI)uri).body((Object)new RequisicaoNovoUser(user));
        }
        if (userOptional.isPresent()) {
            return ResponseEntity.badRequest().body((Object)new ErrorCadastroDTO("Este Email j\u00e1 \u00e9sta cadastrado!"));
        }
        if (!requisicaoNovoUser.getSenhaUser().equals(requisicaoNovoUser.getSenhaUser2())) {
            return ResponseEntity.badRequest().body((Object)new ErrorCadastroDTO("As senhas n\u00e3o correspondem!"));
        }
        return ResponseEntity.badRequest().body((Object)new ErrorCadastroDTO("Ops, aconteceu um erro inesperado!"));
    }
}
