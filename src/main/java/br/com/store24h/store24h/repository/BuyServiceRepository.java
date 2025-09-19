/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 *  org.springframework.transaction.annotation.Transactional
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.CompraServiso;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BuyServiceRepository
extends JpaRepository<CompraServiso, Long> {
    public CompraServiso findByIdActivation(Long var1);

    public Page<CompraServiso> findByLocalDateTimeBetweenAndIdUser(LocalDateTime var1, LocalDateTime var2, Long var3, Pageable var4);

    public Page<CompraServiso> findByIdUserAndVisible(Long var1, int var2, Pageable var3);

    public Page<CompraServiso> findByIdUser(Long var1, Pageable var2);

    @Query(value="SELECT c.id FROM CompraServiso c WHERE c.idActivation = :idActivation ORDER BY c.id DESC")
    public Optional<Long> findLatestIdByIdActivation(@Param(value="idActivation") Long var1);

    @Query(value="SELECT c.id FROM registro_de_compras c WHERE c.id_activation = :idActivation ORDER BY c.id DESC LIMIT 1", nativeQuery=true)
    public Optional<Long> findTopLatestIdByIdActivation(@Param(value="idActivation") Long var1);

    @Modifying
    @Transactional
    @Query(value="UPDATE CompraServiso c SET c.sms = :sms, c.status = :status, c.visible = :visible WHERE c.id = :id")
    public void updateById(@Param(value="id") Long var1, @Param(value="sms") String var2, @Param(value="status") int var3, @Param(value="visible") int var4);

    @Modifying
    @Transactional
    @Query(value="UPDATE CompraServiso c SET c.status = :status, c.visible = :visible WHERE c.id = :id")
    public void updateById(@Param(value="id") Long var1, @Param(value="status") int var2, @Param(value="visible") int var3);

    @Modifying
    @Transactional
    @Query(value="UPDATE CompraServiso c SET c.sms = :sms, c.status = :status, c.visible = :visible WHERE c.idActivation = :idActivation ")
    public void updateByIdActivation(@Param(value="idActivation") Long var1, @Param(value="sms") String var2, @Param(value="status") int var3, @Param(value="visible") int var4);

    @Modifying
    @Transactional
    @Query(value="UPDATE CompraServiso c SET  c.status = :status, c.visible = :visible WHERE c.idActivation = :idActivation ")
    public void updateByIdActivation(@Param(value="idActivation") Long var1, @Param(value="status") int var2, @Param(value="visible") int var3);

    public Page<CompraServiso> findByLocalDateTimeBetween(LocalDateTime var1, LocalDateTime var2, Pageable var3);
}
