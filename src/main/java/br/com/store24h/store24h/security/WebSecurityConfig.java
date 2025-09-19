/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.Filter
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.security.authentication.AuthenticationManager
 *  org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
 *  org.springframework.security.config.annotation.web.builders.HttpSecurity
 *  org.springframework.security.config.annotation.web.builders.WebSecurity
 *  org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 *  org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
 *  org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer$AuthorizedUrl
 *  org.springframework.security.config.http.SessionCreationPolicy
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.security.crypto.password.PasswordEncoder
 *  org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
 *  org.springframework.security.web.SecurityFilterChain
 */
package br.com.store24h.store24h.security;

import br.com.store24h.store24h.model.Role;
import br.com.store24h.store24h.repository.AdmDbRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import br.com.store24h.store24h.security.AutenticacaoService;
import br.com.store24h.store24h.security.AutheticacaoViaTokenFilter;
import br.com.store24h.store24h.security.TokenApp;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig
extends WebSecurityConfigurerAdapter {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private TokenApp tokenApp;
    @Autowired
    private AutenticacaoService autenticacaoService;
    @Autowired
    private UserDbRepository jogadorRepository;
    @Autowired
    private AdmDbRepository admRepository;

    @Bean
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(this.userDetailsService).passwordEncoder((PasswordEncoder)new BCryptPasswordEncoder());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
            .authorizeRequests()
            .antMatchers(
                "/api-docs/**",
                "/docs/**",
                "/api/**",
                "/health",
                "/health/",
                "/health/**",
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
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(new AutheticacaoViaTokenFilter(this.tokenApp, this.jogadorRepository, this.admRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    public void configure(WebSecurity web) throws Exception {
    }
}
