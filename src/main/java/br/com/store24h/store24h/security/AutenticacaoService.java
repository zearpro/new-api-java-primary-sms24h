/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.security.core.userdetails.UserDetails
 *  org.springframework.security.core.userdetails.UserDetailsService
 *  org.springframework.security.core.userdetails.UsernameNotFoundException
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.security;

import br.com.store24h.store24h.model.Administrador;
import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.AdmDbRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import java.util.HashSet;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service(value="userDetailsService")
public class AutenticacaoService
implements UserDetailsService {
    @Autowired
    private AdmDbRepository admRepository;
    @Autowired
    private UserDbRepository userDbRepository;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = this.userDbRepository.findByEmail(username);
        Optional<Administrador> adm = this.admRepository.findByEmail(username);
        if (user.isPresent()) {
            User userLogado = user.get();
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userLogado.getPerfil());
            HashSet<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
            authorities.add(authority);
            return userLogado;
        }
        if (adm.isPresent()) {
            Administrador administrador = adm.get();
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(administrador.getPerfil());
            HashSet<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
            authorities.add(authority);
            return administrador;
        }
        throw new UsernameNotFoundException("dados invalidos");
    }
}
