package br.com.store24h.store24h.security;

import br.com.store24h.store24h.model.Role;
import br.com.store24h.store24h.repository.AdmDbRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private TokenApp tokenApp;

    @Autowired
    private UserDbRepository jogadorRepository;

    @Autowired
    private AdmDbRepository admRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> authz
                .antMatchers(
                    "/",
                    "/api-docs/**",
                    "/docs/**",
                    "/docs/swagger-ui/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/api/**",
                    "/health",
                    "/health/",
                    "/health/**",
                    "/actuator/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/smshub",
                    "/internal/running",
                    "/stubs/handler_api/ping",
                    "/stubs/handler_api/getSms",
                    "/stubs/handler_api/conclude/activation/**",
                    "/stubs/handler_api/cancel/activation/**",
                    "/stubs/handler_api/auth/login",
                    "/stubs/handler_api/createADM",
                    "/stubs/handler_api/createKeyApiMD5",
                    "/stubs/handler_api/createUser",
                    "/stubs/handler_api/auth/login/user",
                    "/stubs/handler_api/getNumberStatux",
                    "/stubs/handler_api/getBalance",
                    "/stubs/handler_api/getNumber",
                    "/stubs/handler_api/getNumberStatus",
                    "/stubs/handler_api/status",
                    "/stubs/handler_api/setStatus",
                    "/stubs/handler_api/prices",
                    "/stubs/handler_api/listaDePaisesOperadoras/**",
                    "/stubs/handler_api/listServicos",
                    "/stubs/handler_api/apiServicos/activity",
                    "/stubs/handler_api/apiServicos/newService",
                    "/stubs/handler_api/apiServicos/getAllServices",
                    "/stubs/handler_api/apiServicos/getAllServices/hub",
                    "/stubs/handler_api/apiServicos/setActivityServices/hub",
                    "/stubs/handler_api/apiServicos/get/all/services",
                    "/stubs/handler_api/apiServicos/getService/**",
                    "/stubs/handler_api/apiServicos/editService/**",
                    "/stubs/handler_api/apiServicos/deleteService/**",
                    "/stubs/handler_api/apiServicos/getComprasFeitas",
                    "/stubs/handler_api/apiServicos/comprarServico/**",
                    "/stubs/handler_api/getTableCredito",
                    "/stubs/handler_api"
                ).permitAll()
                .antMatchers(
                    "/stubs/handler_api/activations",
                    "/stubs/handler_api/activations/**",
                    "/stubs/handler_api/testartoken",
                    "/stubs/handler_api/userDetails",
                    "/stubs/handler_api/criarChaveApi",
                    "/stubs/handler_api/apiServicos/edit/price/**",
                    "/stubs/handler_api/apiServicos/getComprasFeitas/hub",
                    "/stubs/handler_api/apiServicos/getComprasFeitas/hub/filter",
                    "/stubs/handler_api/getCredito",
                    "/stubs/handler_api/comprarCredito",
                    "/stubs/handler_api/edit/password",
                    "/stubs/handler_api/getApiKey"
                ).hasAuthority(Role.USER.getNome())
                .anyRequest().authenticated()
            )
            .addFilterBefore(new AutheticacaoViaTokenFilter(tokenApp, jogadorRepository, admRepository), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}