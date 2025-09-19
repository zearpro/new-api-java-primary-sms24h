/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.ChipOther;
import br.com.store24h.store24h.repository.ChipOtherRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtherService {
    @Autowired
    private ChipOtherRepository otherRepository;

    public boolean isIn(String chipNumber) {
        Optional<ChipOther> otherOptional = this.otherRepository.findByNumber(chipNumber);
        return otherOptional.isPresent();
    }

    @Transactional
    public void remove(String chipNumber) {
        this.otherRepository.deleteByNumber(chipNumber);
    }

    public ChipOther save(String number) {
        ChipOther other = new ChipOther();
        other.setNumber(number);
        return (ChipOther)this.otherRepository.save(other);
    }
}
