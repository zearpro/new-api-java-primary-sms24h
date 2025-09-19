/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.ApiKey;

import br.com.store24h.store24h.model.TimeZone;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/stubs/handler_api/"})
public class CreateAndUpdateAPIKey {
    @RequestMapping(value={"/createKeyApiMD5"})
    public ResponseEntity<String> generateMD5Hashvalue() {
        MessageDigest md;
        String userName = "UserSms24h";
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
        return ResponseEntity.ok(password);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
