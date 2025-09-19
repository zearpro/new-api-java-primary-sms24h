/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.validation.Valid
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.authentication.AuthenticationManager
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.AuthenticationException
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api;

import br.com.store24h.store24h.dto.TokenDTO;
import br.com.store24h.store24h.form.LoginForm;
import br.com.store24h.store24h.security.TokenApp;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class Login {
    @Autowired
    private AuthenticationManager authManager;
    @Autowired
    private TokenApp tokenApp;

    @PostMapping(value={"/auth/login"})
    public ResponseEntity<TokenDTO> autenticarAdm(@RequestBody @Valid LoginForm form) {
        UsernamePasswordAuthenticationToken dadosLogin = form.converter();
        try {
            Authentication authentication = this.authManager.authenticate((Authentication)dadosLogin);
            String token = this.tokenApp.gerarTokenAdm(authentication);
            return ResponseEntity.ok(new TokenDTO(token, "Bearer"));
        }
        catch (AuthenticationException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value={"/auth/login/user"})
    public ResponseEntity<TokenDTO> autenticarUser(@RequestBody @Valid LoginForm form) {
        UsernamePasswordAuthenticationToken dadosLogin = form.converter();
        try {
            Authentication authentication = this.authManager.authenticate((Authentication)dadosLogin);
            String token = this.tokenApp.gerarTokenUser(authentication);
            return ResponseEntity.ok(new TokenDTO(token, "Bearer"));
        }
        catch (AuthenticationException e) {
            System.out.printf(e.getMessage(), new Object[0]);
            return ResponseEntity.badRequest().build();
        }
    }
}
