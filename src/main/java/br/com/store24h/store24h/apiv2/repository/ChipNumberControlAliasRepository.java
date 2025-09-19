/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.EntityManager
 *  javax.persistence.PersistenceContext
 *  javax.persistence.Query
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.apiv2.repository;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.apiv2.model.ChipNumberControlAliasServiceDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class ChipNumberControlAliasRepository {
    @PersistenceContext
    private EntityManager entityManager;
    Logger logger = LoggerFactory.getLogger(ChipNumberControlAliasRepository.class);

    public List<ChipNumberControlAliasServiceDTO> findByChipNumberInAndAliasService(List<String> numeros, String service) {
        return this.findByChipNumberInAndAliasService(numeros, service, null);
    }

    public List<ChipNumberControlAliasServiceDTO> findByChipNumberInAndAliasService(List<String> numeros, String service, LocalDateTime criado) {
        String sql = "SELECT c.chip_number_control_id, c.alias_service, cn.chip_number FROM chip_number_control_alias_service c inner join chip_number_control cn on cn.id=c.chip_number_control_id WHERE  cn.chip_number IN (:numeros) and c.alias_service = :service %s";
        String criadoSql = criado == null ? "" : " and created > :criado";
        sql = String.format(sql, criadoSql);
        String formattedQuery = sql.replace(":numeros", Utils.ListToSql(numeros)).replace(":service", Utils.stringToSQL(service));
        Query query = this.entityManager.createNativeQuery(sql);
        query.setParameter("numeros", numeros);
        query.setParameter("service", (Object)service);
        if (criado != null) {
            query.setParameter("criado", (Object)criado);
            formattedQuery = formattedQuery.replace(":criado", Utils.LocalDateTimeToSql(criado));
        }
        formattedQuery = formattedQuery.replace("[", "(").replace("]", ")").replaceAll(",\\s+", ", ").replace("\"", "'").replaceAll("\n", " ").replaceAll("   ", " ");
        this.logger.info("RUN_SQL: " + formattedQuery + ";");
        List<Object[]> resultList = query.getResultList();
        return resultList.stream().map(row -> new ChipNumberControlAliasServiceDTO(((Number)row[0]).longValue(), (String)row[1], (String)row[2])).collect(Collectors.toList());
    }
}
