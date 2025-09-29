/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.services.core.VendaWhatsapp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChipRepository
extends JpaRepository<ChipModel, Long> {
    public List<ChipModel> findByStatus(int var1);

    public List<ChipModel> findByAtivoAndStatus(boolean var1, int var2);

    public Optional<ChipModel> findByNumber(String var1);

    public Optional<List<ChipModel>> findByAlugado(Boolean var1);

    public List<ChipModel> findByStatusAndChecked(int var1, boolean var2);

    public List<ChipModel> findByOperadora(String var1);

    public List<ChipModel> findByAtivo(Boolean var1);

    public List<ChipModel> findByAtivoAndOperadora(Boolean var1, String var2);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.alugado = :alugado AND c.ativo = :ativo  and id >= (SELECT FLOOR(RAND() * (SELECT MAX(id) FROM chip_model)))  limit :limite", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivo(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("limite") int limite);

    @Query(value="SELECT c.*\nFROM chip_model c\nWHERE c.alugado = :alugado AND c.ativo = :ativo\nAND c.number NOT IN (\n    SELECT cm.number\n    FROM chip_number_control nc\n    INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n    INNER JOIN chip_model cm ON cm.number = nc.chip_number\n    WHERE al.alias_service = :service AND cm.ativo = 1\n)\nORDER BY RAND()\nLIMIT :limite\n", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndService(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("service") String service, @Param("limite") int limite);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.alugado = :alugado AND c.ativo = :ativo AND c.vendawhatsapp IN :whatsappAtivo ORDER BY RAND()  limit :limite", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndVendawhatsappIn(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("whatsappAtivo") List<String> whatsappAtivo, @Param("limite") int limite);

    @Query(value="SELECT c.*\nFROM chip_model c\nWHERE c.alugado = :alugado\nAND c.ativo = :ativo\nAND c.vendawhatsapp IN (:whatsappAtivo)\nAND c.number NOT IN (\n    SELECT cm.number\n    FROM chip_number_control nc\n    INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n    INNER JOIN chip_model cm ON cm.number = nc.chip_number\n    WHERE al.alias_service = :service\n    AND cm.ativo = 1\n)\nORDER BY RAND()\nLIMIT :limite\n", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndVendawhatsappInAndService(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("whatsappAtivo") List<String> whatsappAtivo, @Param("service") String service, @Param("limite") int limite);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.alugado = :alugado AND c.ativo = :ativo AND c.operadora = :operator ORDER BY RAND() LIMIT :limite", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndOperadora(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("limite") int limite);

    @Query(value="SELECT c.*\nFROM chip_model c\nWHERE c.alugado = :alugado\nAND c.ativo = :ativo\nAND c.operadora = :operator\nAND c.number NOT IN (\n    SELECT cm.number\n    FROM chip_number_control nc\n    INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n    INNER JOIN chip_model cm ON cm.number = nc.chip_number\n    WHERE al.alias_service = :service\n    AND cm.ativo = 1\n)\nORDER BY RAND()\nLIMIT :limite\n", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndOperadoraAndService(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("service") String service, @Param("limite") int limite);

    @Query(value="SELECT c.*\nFROM chip_model c\nWHERE c.alugado = :alugado\nAND c.ativo = :ativo\nAND c.operadora = :operator\nAND c.vendawhatsapp IN (:whatsappAtivo)\nAND c.number NOT IN (\n    SELECT cm.number\n    FROM chip_number_control nc\n    INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id\n    INNER JOIN chip_model cm ON cm.number = nc.chip_number\n    WHERE al.alias_service = :service\n    AND cm.ativo = 1\n)\nORDER BY RAND()\nLIMIT :limite\n", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndOperadoraAndVendawhatsappInAndService(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("whatsappAtivo") List<String> whatsappAtivo, @Param("service") String service, @Param("limite") int limite);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.alugado = :alugado AND c.ativo = :ativo AND c.operadora = :operator AND c.vendawhatsapp IN :whatsappAtivo ORDER BY RAND()  limit :limite", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoAndOperadoraAndVendawhatsappIn(@Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("whatsappAtivo") List<String> whatsappAtivo, @Param("limite") int limite);

    public List<ChipModel> findByAlugadoAndAtivoAndVendawhatsapp(Boolean var1, Boolean var2, VendaWhatsapp var3);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.alugado = :alugado AND c.ativo = :ativo ORDER BY RAND() ", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoRandomOrderWithLimit(@Param(value="alugado") Boolean var1, @Param(value="ativo") Boolean var2, Pageable var3);

    @Query(value="SELECT * FROM chip_model WHERE alugado = :alugado AND ativo = :ativo ORDER BY RAND() LIMIT 150", nativeQuery=true)
    public List<ChipModel> findByAlugadoAndAtivoRandomOrderWithLimit(@Param(value="alugado") Boolean var1, @Param(value="ativo") Boolean var2);

    public List<ChipModel> findByAlugadoAndAtivoAndNumberNotIn(Boolean var1, Boolean var2, List<String> var3);

    public List<ChipModel> findByAlugadoAndAtivoAndNumberNotIn(Boolean var1, Boolean var2, List<String> var3, Pageable var4);

    public List<ChipModel> findByAlugadoAndAtivoAndNumberIn(Boolean var1, Boolean var2, List<String> var3);

    public List<ChipModel> findByAlugadoAndAtivoAndOperadoraAndVendawhatsapp(Boolean var1, Boolean var2, String var3, VendaWhatsapp var4);

    public List<ChipModel> findByAlugadoAndAtivoAndOperadora(Boolean var1, Boolean var2, String var3, Pageable var4);

    // ✅ Country-aware queries for enhanced filtering
    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivo(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo AND c.operadora = :operator ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivoAndOperadora(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo AND c.number NOT IN (SELECT cm.number FROM chip_number_control nc INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id INNER JOIN chip_model cm ON cm.number = nc.chip_number WHERE al.alias_service = :service AND cm.ativo = 1) ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivoAndService(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("service") String service);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo AND c.operadora = :operator AND c.number NOT IN (SELECT cm.number FROM chip_number_control nc INNER JOIN chip_number_control_alias_service al ON al.chip_number_control_id = nc.id INNER JOIN chip_model cm ON cm.number = nc.chip_number WHERE al.alias_service = :service AND cm.ativo = 1) ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivoAndOperadoraAndService(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("service") String service);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo AND c.vendawhatsapp IN :whatsappAtivo ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivoAndVendawhatsappIn(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("whatsappAtivo") List<String> whatsappAtivo);

    @Query(value="SELECT c.* FROM chip_model c WHERE c.country = :country AND c.alugado = :alugado AND c.ativo = :ativo AND c.operadora = :operator AND c.vendawhatsapp IN :whatsappAtivo ORDER BY RAND() LIMIT 1000", nativeQuery=true)
    public List<ChipModel> findByCountryAndAlugadoAndAtivoAndOperadoraAndVendawhatsappIn(@Param("country") String country, @Param("alugado") Boolean alugado, @Param("ativo") Boolean ativo, @Param("operator") String operator, @Param("whatsappAtivo") List<String> whatsappAtivo);
}
