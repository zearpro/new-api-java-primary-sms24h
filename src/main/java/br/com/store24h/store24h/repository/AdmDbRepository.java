/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.Administrador;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdmDbRepository
extends JpaRepository<Administrador, Long> {
    public Optional<Administrador> findByEmail(String var1);
}
