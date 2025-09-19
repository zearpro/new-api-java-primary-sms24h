/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.services.core;

import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.repository.ServicosRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServicesHubService {
    @Autowired
    private ServicosRepository servicosRepository;

    public Optional<Servico> getService(String aliasService) {
        Optional<Servico> servico = this.servicosRepository.findFirstByAlias(aliasService);
        return servico;
    }
}
