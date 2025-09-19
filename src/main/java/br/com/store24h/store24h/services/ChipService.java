/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.model.ChipModel;
import br.com.store24h.store24h.model.StatusChipModel;
import br.com.store24h.store24h.repository.ChipRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChipService {
    @Autowired
    private ChipRepository chipRepository;

    public void activityChips(List<ChipModel> chipModelList) {
        ArrayList chipModelModify = new ArrayList();
        chipModelList.forEach(chipModel -> {
            chipModel.setAtivo(true);
            chipModel.setStatus(StatusChipModel.ACTIVITY.getStatus());
            chipModelModify.add(chipModel);
        });
        this.chipRepository.saveAll(chipModelModify);
    }

    public void reset() {
        List<ChipModel> chipModelList = this.chipRepository.findAll();
        ArrayList<ChipModel> chipModelModify = new ArrayList<ChipModel>();
        chipModelList.forEach(chipModel -> {
            chipModel.setAtivo(false);
            chipModel.setStatus(StatusChipModel.NOACTIVITY.getStatus());
            chipModelModify.add(chipModel);
        });
        this.chipRepository.saveAll(chipModelModify);
    }
}
