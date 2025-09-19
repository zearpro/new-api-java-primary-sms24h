/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.client.RestTemplate
 */
package br.com.store24h.store24h.Funcionalidades;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CronCheck {
    public static boolean canRunCron(String cronname) {
        // PRODUCTION SAFETY: All cron tasks are permanently disabled
        System.out.println("⚠️  CRON DISABLED: " + cronname + " - All scheduled tasks disabled for production");
        return false;

        // Original cron logic commented out for production safety:
        /*
        String forceCron = System.getenv("FORCE_CRON");
        String apenas = System.getenv("ONLY");
        if (apenas != null) {
            if (apenas.equals(cronname)) {
                System.out.println("rodando cron apenas para " + cronname);
                return true;
            }
            System.out.println("rodando cron apenas para " + apenas + " nao rodar o cron " + cronname);
            return false;
        }
        if (System.getenv("DISABLE_" + cronname) != null) {
            System.out.println("cron para " + cronname + " desligado");
            return false;
        }
        if (forceCron != null) {
            System.out.println("rodando cron forcado para " + cronname);
            return true;
        }
        String javaHome = System.getenv("NOT_CRON");
        if (javaHome != null) {
            return false;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity responseEntity = restTemplate.getForEntity("http://rancher-metadata/latest/self/service/containers", String.class, new Object[0]);
            ResponseEntity responseEntityService = restTemplate.getForEntity("http://rancher-metadata/latest/self/container/name", String.class, new Object[0]);
            String containers = (String)responseEntity.getBody();
            String[] containersList = containers.split("\n");
            String myContainerName = (String)responseEntityService.getBody();
            String primeiroContainer = containersList[0];
            boolean souOprimeiro = primeiroContainer.indexOf(myContainerName) > -1;
            System.out.println(cronname + "--> check cron containers: " + containers + " - myContainerName " + myContainerName + " - primeiroContainer: " + primeiroContainer + " - " + (souOprimeiro ? "rodar" : "NAO RODAR"));
            return souOprimeiro;
        }
        catch (Exception e) {
            return true;
        }
        */
    }
}
