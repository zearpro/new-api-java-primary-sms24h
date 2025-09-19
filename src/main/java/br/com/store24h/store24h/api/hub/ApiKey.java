/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minidev.json.JSONObject
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.api.hub;

import br.com.store24h.store24h.Funcionalidades.Funcionalidades;
import br.com.store24h.store24h.dto.ApiKeyDTO;
import br.com.store24h.store24h.dto.ErrorResponseDto;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.UserDbRepository;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api"})
public class ApiKey {
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private Funcionalidades funcionalidades;

    @GetMapping(value={"/criarChaveApi"})
    public ResponseEntity<?> chaveApi(Authentication authentication) {
        JSONObject myJson = new JSONObject();
        User user = this.funcionalidades.userLogado(authentication);
        if (user.getApiKey() == null) {
            try {
                String apiKey = this.funcionalidades.gerarKeyApi("null");
                user.setApiKey(apiKey);
                this.userDbRepository.save(user);
                myJson.put("apiKey", apiKey);
                return ResponseEntity.ok().body(myJson);
            }
            catch (Exception e) {
                return ResponseEntity.badRequest().body("Ops aconteceu um erro, apiKey n\\u00e3o criada!");
            }
        }
        return ResponseEntity.badRequest().body("Voc\\u00ea n\\u00e3o pode ter mais de uma ApiKey");
    }

    @GetMapping(value={"/getApiKey"})
    public ResponseEntity<Object> getApiKey(Authentication authentication) {
        try {
            User user = this.funcionalidades.userLogado(authentication);
            return ResponseEntity.ok().body(new ApiKeyDTO(user.getApiKey()));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Ops, algo deu errado!"));
        }
    }
}
