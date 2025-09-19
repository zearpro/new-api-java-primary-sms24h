/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.FilterChain
 *  javax.servlet.ServletException
 *  javax.servlet.ServletRequest
 *  javax.servlet.ServletResponse
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.context.SecurityContextHolder
 *  org.springframework.web.filter.OncePerRequestFilter
 */
package br.com.store24h.store24h.security;

import br.com.store24h.store24h.model.Administrador;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.AdmDbRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.security.TokenApp;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AutheticacaoViaTokenFilter
extends OncePerRequestFilter {
    private TokenApp tokenApp;
    private UserDbRepository userDbRepository;
    private AdmDbRepository admDbRepository;
    private static final Logger logger = LogManager.getLogger(AutheticacaoViaTokenFilter.class);

    public AutheticacaoViaTokenFilter(TokenApp tokenApp, UserDbRepository userDbRepository, AdmDbRepository admDbRepository) {
        this.admDbRepository = admDbRepository;
        this.userDbRepository = userDbRepository;
        this.tokenApp = tokenApp;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean valido;
        String token = this.recuperarToken(request);
        if (token != null && (valido = this.tokenApp.isValidToken(token))) {
            String usarioEmail = this.tokenApp.getEmail(token);
            long startTime = System.nanoTime();
            Optional<User> userOptional = this.userDbRepository.findFirstByEmail(usarioEmail);
            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            double durationSeconds = (double)durationNanos / 1.0E9;
            logger.info("[{}]Tempo gasto para a buscar o usuario pelo email {} ", (Object)durationSeconds, (Object)usarioEmail);
            if (userOptional.isPresent()) {
                this.autenticarUser(token);
            } else {
                this.autenticarAdm(token);
            }
        }
        filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
    }

    private void autenticarUser(String token) {
        String emailUser = this.tokenApp.getEmail(token);
        Optional<User> userOpt = this.userDbRepository.findFirstByEmail(emailUser);
        userOpt.get();
        User user = userOpt.get();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken((Object)user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication((Authentication)authentication);
    }

    private void autenticarAdm(String token) {
        String idAdm = this.tokenApp.getEmail(token);
        Administrador adm = this.admDbRepository.findByEmail(idAdm).get();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken((Object)adm, null, adm.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication((Authentication)authentication);
    }

    public static String getBody(HttpServletRequest request) throws IOException {
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private String recuperarToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty() || !token.startsWith("Bearer ")) {
            return null;
        }
        return token.substring(7, token.length());
    }
}
