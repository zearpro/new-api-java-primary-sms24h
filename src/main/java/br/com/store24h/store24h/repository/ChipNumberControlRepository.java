/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.ChipNumberControl;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChipNumberControlRepository
extends JpaRepository<ChipNumberControl, Long> {
    public List<ChipNumberControl> findByChipNumberIn(List<String> var1);

    public Optional<ChipNumberControl> findByChipNumber(String var1);

    @Query(value="SELECT c.* FROM chip_number_control_alias_service c inner join chip_number_control cn on cn.id=c.chip_number_control_id WHERE  cn.chip_number IN :chipNumbers and c.alias_service = :aliasService", nativeQuery=true)
    public List<ChipNumberControl> findByChipNumberInAndAliasService(@Param("chipNumbers") List<String> chipNumbers, @Param("aliasService") String aliasService);

    public Optional<ChipNumberControl> findByChipNumberAndAliasService(String var1, String var2);
}
