/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.models.OpenAPI
 *  io.swagger.v3.oas.models.info.Info
 *  io.swagger.v3.oas.models.servers.Server
 *  org.springdoc.core.GroupedOpenApi
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package br.com.store24h.store24h;

// update
import br.com.store24h.store24h.Utils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        boolean isHomolog = System.getenv("IS_HOMOLOG") != null;
        boolean isLocal = System.getenv("IS_LOCAL") != null;
        ArrayList servers = new ArrayList();
        List.of("http://localhost:80/", "http://dev.sms24h.com/", "https://api.sms24h.org/").forEach(value -> {
            Server server = new Server();
            server.setUrl(value);
            servers.add(server);
        });
        String descricao = isHomolog ? "API Documentation - AMBIENTE DE HOMOLOGACAO" : "API Documentation";
        descricao = "API Documentation";
        return new OpenAPI().info(new Info().title(descricao).version(Utils.getVersionContainer()));
    }

    @Bean
    public GroupedOpenApi smshubApi() {
        return GroupedOpenApi.builder().group("SMSHUB").pathsToMatch(new String[]{"/smshub"}).build();
    }

    @Bean
    public GroupedOpenApi sistemas() {
        return GroupedOpenApi.builder().group("SISTEMAS").pathsToMatch(new String[]{"/api/**"}).build();
    }

    // Removed v1Api group - test endpoints removed

    @Bean
    public GroupedOpenApi healthApi() {
        return GroupedOpenApi.builder().group("health").pathsToMatch(new String[]{"/health/**"}).build();
    }
}
