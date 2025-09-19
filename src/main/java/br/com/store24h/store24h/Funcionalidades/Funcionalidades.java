/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.nimbusds.jose.shaded.json.JSONObject
 *  org.springframework.security.core.Authentication
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.Funcionalidades;

import br.com.store24h.store24h.model.Administrador;
import br.com.store24h.store24h.model.TimeZone;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.UserDbRepository;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class Funcionalidades {
    public Administrador admLogado(Authentication authentication) {
        Administrador adm = null;
        if (authentication.getPrincipal() instanceof Administrador) {
            adm = (Administrador)authentication.getPrincipal();
        }
        return adm;
    }

    public String gerarKeyApi(String nomeUser) {
        MessageDigest md;
        String userName = nomeUser;
        LocalDate dateObj = LocalDate.now(ZoneId.of(TimeZone.BR.getZone()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = dateObj.format(formatter);
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        String secretPhase = "geeks";
        byte[] hashResult = md.digest((date + userName + secretPhase).getBytes(StandardCharsets.UTF_8));
        String password = this.bytesToHex(hashResult);
        return password;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public User userLogado(Authentication authentication) {
        User user = null;
        if (authentication.getPrincipal() instanceof User) {
            user = (User)authentication.getPrincipal();
        }
        return user;
    }

    public void addCredito(UserDbRepository userDbRepository, BigDecimal credito, Authentication authentication) {
        User user = this.userLogado(authentication);
        BigDecimal newCredito = user.getCredito().add(credito);
        user.setCredito(newCredito);
        userDbRepository.save(user);
    }

    public String getNumumeroDisponivel() {
        ArrayList<String> numerosDiponives = new ArrayList<String>();
        numerosDiponives.add("+55 66 9 8336-3821");
        numerosDiponives.add("+55 21 9 8323-4367");
        numerosDiponives.add("+55 93 9 9652-5671");
        numerosDiponives.add("+55 21 9 8237-6532");
        numerosDiponives.add("+55 11 9 8523-3231");
        numerosDiponives.add("+55 21 9 9627-5476");
        numerosDiponives.add("+55 91 9 8653-3438");
        numerosDiponives.add("+55 21 9 8336-4786");
        numerosDiponives.add("+55 11 9 9833-8634");
        numerosDiponives.add("+55 21 9 8834-3821");
        int result = (int)ThreadLocalRandom.current().nextLong(0L, 9L);
        return (String)numerosDiponives.get(result);
    }

    public JSONObject getNumeroStatus() {
        ArrayList<JSONObject> listJson = new ArrayList<JSONObject>();
        JSONObject myJson = new JSONObject();
        int index = (int)ThreadLocalRandom.current().nextLong(0L, 3L);
        int codigo = (int)ThreadLocalRandom.current().nextLong(1000L, 9999L);
        myJson.put("STATUS_WAIT_CODE", "Estamos aguardando a chegada de SMS");
        listJson.add(myJson);
        myJson.put("STATUS_WAIT_RETRY: ", "" + codigo);
        listJson.add(myJson);
        myJson.put("STATUS_CANCEL: ", "ativação cancelada ");
        listJson.add(myJson);
        myJson.put("STATUS_OK: ", "" + codigo);
        listJson.add(myJson);
        return (JSONObject)listJson.get(index);
    }
}
