/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package br.com.store24h.store24h.repository;

import br.com.store24h.store24h.model.SmsModel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsRepository
extends JpaRepository<SmsModel, Long> {
    public List<SmsModel> findByChipnumberAndIdActivation(String var1, Long var2);

    public List<SmsModel> findByChipnumberAndIdActivationOrderByIdDesc(String var1, Long var2);

    public Optional<SmsModel> findByIdActivation(Long var1, Sort var2);

    public Optional<SmsModel> findFirstByIdActivationOrderByDateDesc(Long var1);

    public Optional<SmsModel> findFirstByChipnumberAndIdActivationOrderByDateDesc(String var1, Long var2);

    public Optional<SmsModel> findFirstByChipnumberAndIdActivationAndPretryOrderByDateDesc(String var1, Long var2, int var3);

    public Optional<SmsModel> findFirstByChipnumberAndIdActivationOrderByIdDesc(String var1, Long var2);

    public List<SmsModel> findByChipnumberAndIdActivationAndPretryOrderByDateDesc(String var1, Long var2, int var3);

    public List<SmsModel> findByChipnumberAndIdActivationOrderByDateDesc(String var1, Long var2);

    public SmsModel findByMsgAndIdActivation(String var1, Long var2);

    public Optional<SmsModel> findByDateAfterAndIdActivation(LocalDateTime var1, Long var2);

    public List<SmsModel> findByDateBefore(LocalDateTime var1);

    public Optional<SmsModel> findByDateGreaterThanAndIdActivation(LocalDateTime var1, Long var2);

    public void deleteByChipnumber(String var1);

    public void deleteByIdActivation(Long var1);

    public SmsModel findFirstByIdActivation(long var1, Sort var3);
}
