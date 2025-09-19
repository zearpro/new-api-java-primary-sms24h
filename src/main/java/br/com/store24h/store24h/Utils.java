/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.http.Header
 *  org.apache.http.HttpEntity
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpPost
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.entity.StringEntity
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClients
 *  org.apache.http.util.EntityUtils
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package br.com.store24h.store24h;

import br.com.store24h.store24h.VersionEnum;
import br.com.store24h.store24h.apiv2.TipoDeApiEnum;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.services.core.TipoDeApiNotPermitedException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    static Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final String NUMERIC_CHARACTERS = "0123456789";

    public static int getVersionNumber() {
        if (Utils.getVersion().equals((Object)VersionEnum.VERSION_2)) {
            return 2;
        }
        return 1;
    }

    public static boolean checkUserApiType(CacheService cacheService, String apiKey, TipoDeApiEnum tipoDeApiEnum) throws TipoDeApiNotPermitedException {
        String apiType = cacheService.findUserApiType(apiKey);
        if (apiType != null && apiType.equals(tipoDeApiEnum.name())) {
            return true;
        }
        return true;
    }

    public static VersionEnum getVersion() {
        String version = System.getenv("VERSION");
        System.out.println("LOG_VERSAO ->env :[" + (version == null ? "null" : version) + "]");
        if (version != null && version.trim().equals("2")) {
            System.out.println("LOG_VERSAO -> RETORNA VERSION_2");
            return VersionEnum.VERSION_2;
        }
        System.out.println("LOG_VERSAO -> RETORNA LEGADO");
        return VersionEnum.LEGADO;
    }

    public static String getSingleLineStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        return stackTrace.replaceAll("\\r?\\n", " ");
    }

    public static String diffTimeMs(long startTimeOperacao) {
        long endTime = System.nanoTime();
        long durationOperacaoNano = endTime - startTimeOperacao;
        double durationInMillisOperacao = (double)durationOperacaoNano / 1000000.0;
        double durationOperacaoSegundos = (double)durationOperacaoNano / 1.0E9;
        if (durationInMillisOperacao < 0.0) {
            return String.format("%s/ms", String.format("%.3f", durationInMillisOperacao));
        }
        return String.format("%s/ms", String.format("%.0f", durationInMillisOperacao));
    }

    public static String diffTimeSec(long startTimeOperacao) {
        long endTime = System.nanoTime();
        long durationOperacaoNano = endTime - startTimeOperacao;
        double durationOperacaoSegundos = (double)durationOperacaoNano / 1.0E9;
        return String.format("%s/ms", String.format("%.3f", durationOperacaoSegundos));
    }

    public static String calcTime(long startTimeOperacao, long startTimeTrecho, String message) {
        long endTime = System.nanoTime();
        long durationOperacaoNano = endTime - startTimeOperacao;
        double durationInMillisOperacao = (double)durationOperacaoNano / 1000000.0;
        double durationOperacaoSegundos = (double)durationOperacaoNano / 1.0E9;
        long durationTrechoNano = endTime - startTimeTrecho;
        double durationInMillisTrecho = (double)durationTrechoNano / 1000000.0;
        double durationTrechoSegundos = (double)durationTrechoNano / 1.0E9;
        logger.info("CALCTIME: TEMPO GASTO ATE AGORA: [{}/seg {}/ms]|TEMPO GASTO PARA({}): [{} s/ {}/ms]", new Object[]{String.format("%.2f", durationOperacaoSegundos), durationInMillisOperacao, message, String.format("%.2f", durationTrechoSegundos), durationInMillisTrecho});
        return String.format("%s/ms", String.format("%.3f", durationInMillisTrecho));
    }

    public static String getVersionContainer() {
        try {
            Path filePath = Path.of("/usr/src/version.txt", new String[0]);
            String content = new String(Files.readAllBytes(filePath));
            return content.trim();
        }
        catch (Exception e) {
            return "version-desc";
        }
    }

    public static String ListToSql(List<String> filtroDeNumerosParaWhatsApp) {
        return String.format("\"%s\"", filtroDeNumerosParaWhatsApp.stream().collect(Collectors.joining("\",\"")));
    }

    public static String stringToSQL(String s) {
        return String.format("\"%s\"", s);
    }

    public static CharSequence stringToSQL(int alugado) {
        return String.valueOf(alugado);
    }

    public static String LocalDateTimeToSql(LocalDateTime criado) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = criado.format(formatter);
        return String.format("\"%s\"", formattedDateTime);
    }

    public static int getLimiteTotalCancelamento() {
        String limiteTotalStr = Objects.requireNonNullElse(System.getenv("LIMITE_TOTAL_CANCELAMENTO"), "1000");
        return Integer.parseInt(limiteTotalStr);
    }

    public static int getThreadCancelamento() {
        String limiteTotalStr = Objects.requireNonNullElse(System.getenv("THREAD_CANCELAMENTO"), "5");
        return Integer.parseInt(limiteTotalStr);
    }

    public static long getDiasParaCancelamento() {
        String limiteTotalStr = Objects.requireNonNullElse(System.getenv("DIAS_PARA_CANCELAMENTO"), "1");
        return Integer.parseInt(limiteTotalStr);
    }

    public static String generateRandomNumberAsString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            int index = 9;
            index = i == 2 ? 9 : (i < 4 ? random.nextInt(1, NUMERIC_CHARACTERS.length()) : random.nextInt(NUMERIC_CHARACTERS.length()));
            stringBuilder.append(NUMERIC_CHARACTERS.charAt(index));
        }
        return stringBuilder.toString();
    }

    public static JSONObject sendHttpRequest(String urlString, String method, Map<String, String> headers, String requestBody) throws Exception {
        HttpUriRequest request;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        if (method.toUpperCase().equals("POST")) {
            HttpPost postRequest = new HttpPost(urlString);
            StringEntity entity = new StringEntity(requestBody);
            postRequest.setEntity(entity);
            postRequest.setHeader("Content-Type", "application/json");
            request = postRequest;
        } else {
            request = new HttpGet(urlString);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
        CloseableHttpResponse response = httpClient.execute(request);
        JSONObject jSONObject = new JSONObject();
        int statusCode = response.getStatusLine().getStatusCode();
        Header[] respHeaders = response.getAllHeaders();
        JSONObject respHeadersJson = new JSONObject();
        for (Header header : respHeaders) {
            respHeadersJson.put(header.getName(), (Object)header.getValue());
        }
        String responseBody = EntityUtils.toString((HttpEntity)response.getEntity());
        jSONObject.put("status_code", statusCode);
        try {
            jSONObject.put("body", (Object)new JSONObject(responseBody));
        }
        catch (Exception e) {
            jSONObject.put("body", (Object)responseBody);
        }
        jSONObject.put("headers", (Object)respHeadersJson);
        JSONObject responseObj = new JSONObject();
        httpClient.close();
        return jSONObject;
    }

    public static boolean isHomolog() {
        boolean eh = System.getenv("IS_HOMOLOG") != null;
        logger.info("CHECK IS HOMOLOG {}", (Object)eh);
        return eh;
    }
}
