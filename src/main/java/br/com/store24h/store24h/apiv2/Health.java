/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package br.com.store24h.store24h.apiv2;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.apiv2.services.NumerosService;
import br.com.store24h.store24h.services.core.VendaWhatsapp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/health"})
public class Health {
    private static final Logger logger = LogManager.getLogger(Health.class);
    @Autowired
    NumerosService numerosService;
    @Autowired
    CacheService cacheService;

    @GetMapping(value={""})
    public ResponseEntity<String> runing2(HttpServletRequest request) {
        return this.runing(request, Optional.empty());
    }

    @GetMapping(value={"/"})
    public ResponseEntity<String> runing(HttpServletRequest request, Optional<String> act) {
        long startTimeOperacao = System.nanoTime();
        long startTimeTrecho = System.nanoTime();
        List<List<String>> filtros = List.of(List.of(VendaWhatsapp.HABILITADOS.name(), VendaWhatsapp.TODOS.name()), List.of(VendaWhatsapp.TODOS.name()));
        logger.info("|----- request -------");
        for (List<String> fltr : filtros) {
            boolean stop = false;
            logger.info("|    ----------" + fltr + "----------");
            for (boolean isWa : List.of(Boolean.valueOf(true), Boolean.valueOf(false))) {
                String strisWa = isWa ? "FILTRO WHATS" : "SEM FILTRO WHATS";
                logger.info("|         -------" + strisWa + "--------");
                logger.info("|        [{}][{}] Checar cache", (Object)strisWa, fltr);
                startTimeTrecho = System.nanoTime();
                List<String> resp = this.cacheService.getLatestNumerosDisponiveisSemFiltrarNumerosPreviosCache(0, 1, isWa, Optional.empty(), fltr, startTimeOperacao, Optional.empty());
                logger.info("|        [{}][{}]  buscou {} numeros| Tempo Gasto para buscar: {}", (Object)strisWa, fltr, (Object)resp.size(), (Object)Utils.diffTimeMs(startTimeTrecho));
                if (resp.size() <= 0) continue;
                this.cacheService.clearNumerosDisponiveisCache(0, 1, isWa, Optional.empty(), fltr, startTimeOperacao, Optional.empty());
            }
            System.out.println("");
            System.out.println("");
            System.out.println("");
        }
        if (act.isPresent()) {
            request.setAttribute("activationId", (Object)String.valueOf(System.currentTimeMillis()));
        }
        try {
            String content = new String(Files.readAllBytes(Paths.get("/usr/src/version.txt", new String[0])));
            return ResponseEntity.ok("OK [" + content.trim() + "]  [" + logger.getLevel() + "]");
        }
        catch (IOException iOException) {
            return ResponseEntity.ok("OK [v.0]   [" + logger.getLevel() + "]");
        }
    }
}
