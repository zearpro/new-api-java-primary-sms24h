/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.Valid
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub.user;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.Requisicoes.RequisitionNewPassword;
import br.com.store24h.store24h.dto.UserDTO;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.services.UserService;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class UserApi {
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private Funcionalidades funcionalidades;
    @Autowired
    private UserService userService;

    @GetMapping(value={"/userDetails"})
    public ResponseEntity<UserDTO> userDatails(Authentication authentication) {
        User user = this.funcionalidades.userLogado(authentication);
        UserDTO userDTO = new UserDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping(value={"/edit/password"})
    public ResponseEntity<String> editPassword(@RequestBody @Valid RequisitionNewPassword requisitionNewPassword, Authentication authentication) {
        try {
            String currentPassword = requisitionNewPassword.getNewPassword();
            if (!currentPassword.equals(requisitionNewPassword.getNewPassword2())) {
                return ResponseEntity.badRequest().body("As senhas n\u00e3o s\u00e3o iguais!");
            }
            if (this.userService.testPassword(currentPassword, authentication)) {
                return ResponseEntity.badRequest().body("Sua senha est\u00e1 incorreta!");
            }
            this.userService.updatePassword(authentication, currentPassword);
            return ResponseEntity.ok().body("Senha alterada!");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body("Ops, Algo deu errado!");
        }
    }
}
