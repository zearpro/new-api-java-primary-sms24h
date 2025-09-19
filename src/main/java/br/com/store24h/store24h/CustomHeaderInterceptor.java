/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.qos.logback.classic.Level
 *  ch.qos.logback.classic.Logger
 *  ch.qos.logback.classic.LoggerContext
 *  javax.annotation.PostConstruct
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.json.JSONObject
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 *  org.springframework.web.servlet.HandlerInterceptor
 *  org.springframework.web.servlet.ModelAndView
 */
package br.com.store24h.store24h;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import java.util.Enumeration;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CustomHeaderInterceptor
implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(CustomHeaderInterceptor.class);
    private static final String START_TIME = "startTime";
    private static final String REQUEST_ID = "requestId";
    public static final String ACTIVATION_ID = "activationId";

    @PostConstruct
    public void setLogLevel() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(CustomHeaderInterceptor.class);
        rootLogger.setLevel(Level.INFO);
        logger.debug("Logging level set to DEBUG for CustomHeaderInterceptor");
    }

    public static JSONObject getHeadersAsJson(HttpServletRequest request) {
        JSONObject headersJson = new JSONObject();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String)headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersJson.put(headerName, (Object)headerValue);
        }
        return headersJson;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME, (Object)startTime);
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        UUID requestId = UUID.randomUUID();
        request.setAttribute(REQUEST_ID, (Object)requestId.toString());
        String headersString = CustomHeaderInterceptor.getHeadersAsJson(request).toString();
        if (path.startsWith("/health")) {
            logger.debug("{}: Request Path: {}, Query String: {} STARTED  - HEADERS: {}", new Object[]{requestId.toString(), path, queryString, headersString});
        } else {
            logger.info("{}: Request Path: {}, Query String: {} STARTED  - HEADERS: {}", new Object[]{requestId.toString(), path, queryString, headersString});
        }
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        long startTime = (Long)request.getAttribute(START_TIME);
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String requestId = (String)request.getAttribute(REQUEST_ID);
        String headersString = CustomHeaderInterceptor.getHeadersAsJson(request).toString();
        String idActivation = (String)request.getAttribute(ACTIVATION_ID);
        String msgAtivacao = "";
        if (idActivation != null) {
            msgAtivacao = String.format("[ATIVACAO=%s]", idActivation);
        }
        if (path.startsWith("/health")) {
            logger.debug("{}:{} Request Path: {}, Query String: {} END, Execution Time: {} ms /{} s  - HEADERS: {}", new Object[]{requestId, msgAtivacao, path, queryString, executeTime, executeTime / 1000L, headersString});
        } else {
            logger.info("{}:{} Request Path: {}, Query String: {} END, Execution Time: {} ms /{} s  - HEADERS: {}", new Object[]{requestId, msgAtivacao, path, queryString, executeTime, executeTime / 1000L, headersString});
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
