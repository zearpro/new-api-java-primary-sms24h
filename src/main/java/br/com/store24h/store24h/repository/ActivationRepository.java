/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.Activation;
import br.com.store24h.store24h.services.core.ActivationStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivationRepository
extends JpaRepository<Activation, Long> {
    public Optional<Activation> findFirstById(Long var1);

    public Optional<Activation> findById(Long var1);

    public Optional<Activation> findByChipNumberAndAliasService(String var1, String var2);

    public List<Activation> findByChipNumber(String var1);

    public List<Activation> findByStatusBuz(ActivationStatus var1);

    public List<Activation> findAllByStatusBuzAndChipNumber(ActivationStatus var1, String var2);

    public List<Activation> findByStatusBuzAndChipNumberAndAliasService(ActivationStatus var1, String var2, String var3);

    public List<Activation> findByStatusBuzInAndChipNumberAndAliasServiceAndVisibleAndVersion(List<ActivationStatus> var1, String var2, String var3, int var4, int var5);

    public List<Activation> findByStatusInAndInitialTimeBefore(List<Integer> var1, LocalDateTime var2);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVisibleAndVersionOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, int var5, Pageable var6);

    public List<Activation> findByStatusInAndInitialTimeAfter(List<Integer> var1, LocalDateTime var2);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVersionAndApiKeyIn(List<Integer> var1, LocalDateTime var2, int var3, List<String> var4);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVersionAndApiKeyNotIn(List<Integer> var1, LocalDateTime var2, int var3, List<String> var4);

    public List<Activation> findByStatusInAndInitialTimeAfterAndApiKeyIn(List<Integer> var1, LocalDateTime var2, List<String> var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndApiKeyNotIn(List<Integer> var1, LocalDateTime var2, List<String> var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVersion(List<Integer> var1, LocalDateTime var2, int var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVisibleAndVersion(List<Integer> var1, LocalDateTime var2, int var3, int var4);

    public List<Activation> findByStatusInAndInitialTimeAfterOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVersionOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, int var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVisibleAndVersionAndApiKeyInOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, int var3, int var4, List<String> var5);

    public List<Activation> findByStatusInAndInitialTimeAfterAndApiKeyInOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, List<String> var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVisibleAndVersionAndApiKeyNotInOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, int var3, int var4, List<String> var5);

    public List<Activation> findByStatusInAndInitialTimeAfterAndApiKeyNotInOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, List<String> var3);

    public List<Activation> findByStatusInAndInitialTimeAfterAndVisibleAndVersionOrderByStatusBuzDesc(List<Integer> var1, LocalDateTime var2, int var3, int var4);

    public List<Activation> findByApiKeyAndInitialTimeAfterAndStatusNotInAndVisibleAndVersion(String var1, LocalDateTime var2, List<Integer> var3, int var4, int var5);

    public List<Activation> findByApiKeyAndVisibleAndVersion(String var1, int var2, int var3);

    public List<Activation> findAllByIdInAndVisibleAndVersion(List<Long> var1, int var2, int var3);

    public List<Activation> findByApiKeyAndVisible(String var1, int var2);

    public long countByStatusInAndInitialTimeBeforeAndInitialTimeAfter(List<Integer> var1, LocalDateTime var2, LocalDateTime var3);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, Pageable var4);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, Pageable var4);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyNotInOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5, Pageable var6);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndApiKeyNotInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, List<String> var4);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyNotInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5);

    public long countByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyIn(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndApiKeyInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, List<String> var4, Pageable var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5, Pageable var6);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndApiKeyInOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, List<String> var4, Pageable var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyInOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5, Pageable var6);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndApiKeyInOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, List<String> var4);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionAndApiKeyInOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, List<String> var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionOrderByIdDesc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4, Pageable var5);

    public List<Activation> findByStatusInAndInitialTimeBeforeAndInitialTimeAfterAndVersionOrderByIdAsc(List<Integer> var1, LocalDateTime var2, LocalDateTime var3, int var4);

    public List<Activation> findByApiKeyAndInitialTimeAfterAndStatusNotInAndVisible(String var1, LocalDateTime var2, List<Integer> var3, int var4);

    public List<Activation> findByApiKeyAndInitialTimeAfterAndStatusNotIn(String var1, LocalDateTime var2, List<Integer> var3);

    public List<Activation> findByStatusBuzInAndChipNumberAndAliasService(ArrayList<ActivationStatus> var1, String var2, String var3);
}
