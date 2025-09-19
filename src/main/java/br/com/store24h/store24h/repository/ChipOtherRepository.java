/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.ChipOther;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChipOtherRepository
extends JpaRepository<ChipOther, Long> {
    public Optional<ChipOther> findByNumber(String var1);

    public void deleteByNumber(String var1);

    public void deleteByDateTimeBefore(LocalDateTime var1);
}
