/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 *  org.springframework.transaction.annotation.Transactional
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserDbRepository
extends JpaRepository<User, Long> {
    public Optional<User> findFirstByEmail(String var1);

    public Optional<User> findByEmail(String var1);

    public Optional<User> findByApiKey(String var1);

    public Optional<User> findFirstByApiKey(String var1);

    @Modifying
    @Transactional
    @Query(value="UPDATE User a SET a.credito = a.credito - :valor WHERE a.apiKey = :apiKey and a.credito >= :valor")
    public int decreaseSaldo(@Param(value="apiKey") String var1, @Param(value="valor") BigDecimal var2);

    @Modifying
    @Transactional
    @Query(value="UPDATE User a SET a.credito = a.credito + :valor WHERE a.apiKey = :apiKey")
    public void increaseSaldo(@Param(value="apiKey") String var1, @Param(value="valor") BigDecimal var2);
}
