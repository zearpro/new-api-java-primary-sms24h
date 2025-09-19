/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.User;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.repository.UserDbRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserDbRepository userDbRepository;
    @Autowired
    private ServicosRepository servicosRepository;

    public User userLogado(Authentication authentication) {
        User user = null;
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User)authentication.getPrincipal();
        }
        return user;
    }

    public void updatePassword(Authentication authentication, String currentPassword) {
        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
        String newPassword = bc.encode((CharSequence)currentPassword);
        User user = this.userLogado(authentication);
        user.setSenha(newPassword);
        this.userDbRepository.save(user);
    }

    public boolean testPassword(String password, Authentication authentication) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = this.userLogado(authentication);
        return passwordEncoder.matches((CharSequence)password, user.getPassword());
    }

    public boolean isValidApiKey(String apiKey) {
        return this.userDbRepository.findFirstByApiKey(apiKey).isPresent();
    }
}
