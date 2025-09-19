/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.Servico;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicosRepository
extends JpaRepository<Servico, Long> {
    public Optional<Servico> findFirstByAlias(String var1);

    public Optional<Servico> findByAlias(String var1);

    public Optional<Servico> findByName(String var1);

    public List<Servico> findAllBySmshub(Integer var1);

    public List<Servico> findByAliasIn(List<String> var1);

    @Async
    public CompletableFuture<List<Servico>> findByActivity(boolean var1, Sort var2);
}
