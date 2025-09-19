/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.Utils;
import br.com.store24h.store24h.model.ChipNumberControl;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.repository.ChipNumberControlRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChipNumberControlService {
    @Autowired
    private ChipNumberControlRepository controlRepository;
    Logger logger = LoggerFactory.getLogger(ChipNumberControlService.class);
    @Autowired
    private ServicosRepository servicosRepository;

    public void addServiceInNumber(String chipNumber, Servico servico) {
        try {
            this.logger.info("[CHIP_NUMBER_CONTROL_SERVICE] [{}] ADICIONAR SERVICE: {}", (Object)chipNumber, (Object)servico.getAlias());
            Optional<ChipNumberControl> chipNumberControlOpt = this.controlRepository.findByChipNumber(chipNumber);
            ChipNumberControl chipNumberControl = !chipNumberControlOpt.isPresent() ? this.newChipNumberControl(chipNumber) : chipNumberControlOpt.get();
            chipNumberControl.getAliasService().add(servico.getAlias());
            this.controlRepository.save(chipNumberControl);
            this.logger.info("[CHIP_NUMBER_CONTROL_SERVICE] [{}] ADICIONADO: {}", (Object)chipNumber, (Object)servico.getAlias());
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC:CHIP_NUMBER_CONTROL_SERVICE {} NUMERO: {} SERVICO:{} STACK: {}", new Object[]{"addService", chipNumber, servico.getAlias(), Utils.getSingleLineStackTrace(e)});
        }
    }

    public ChipNumberControl newChipNumberControl(String chipNumber) {
        ChipNumberControl chipNumberControl = new ChipNumberControl(chipNumber);
        this.controlRepository.save(chipNumberControl);
        return chipNumberControl;
    }

    public void removeService(String chipNumber, String aliasService) {
        try {
            aliasService = aliasService.split("_")[0];
            this.logger.info("[CHIP_NUMBER_CONTROL_SERVICE] [{}] REMOVER SERVICE: {}", (Object)chipNumber, (Object)aliasService);
            Optional<ChipNumberControl> chipNumberControlOpt = this.controlRepository.findByChipNumber(chipNumber);
            if (chipNumberControlOpt.isPresent()) {
                ChipNumberControl chipNumberControl = chipNumberControlOpt.get();
                chipNumberControl.getAliasService().remove(aliasService);
                this.controlRepository.save(chipNumberControl);
                this.logger.info("[CHIP_NUMBER_CONTROL_SERVICE] [{}] REMOVIDO: {}", (Object)chipNumber, (Object)aliasService);
            }
        }
        catch (Exception e) {
            this.logger.info("ERROR_SQL_DESC:CHIP_NUMBER_CONTROL_SERVICE {} NUMERO: {} SERVICO:{} STACK: {}", new Object[]{"removeService", chipNumber, aliasService, Utils.getSingleLineStackTrace(e)});
        }
    }
}
