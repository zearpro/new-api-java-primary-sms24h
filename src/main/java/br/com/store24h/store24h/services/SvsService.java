/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.BeanUtils
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.domain.Sort
 *  org.springframework.stereotype.Service
 */
package br.com.store24h.store24h.services;

import br.com.store24h.store24h.Requisicoes.RequisicaoNovoServico;
import br.com.store24h.store24h.apiv2.services.CacheService;
import br.com.store24h.store24h.model.CompraServiso;
import br.com.store24h.store24h.model.Servico;
import br.com.store24h.store24h.repository.BuyServiceRepository;
import br.com.store24h.store24h.repository.ServicosRepository;
import br.com.store24h.store24h.services.ChipService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SvsService {
    @Autowired
    private ServicosRepository servicosRepository;
    @Autowired
    private BuyServiceRepository buyServiceRepository;
    @Autowired
    private ChipService chipService;
    @Autowired
    private CacheService cacheService;

    public List<Servico> getAllServices() {
        Sort sort = Sort.by((String[])new String[]{"name"});
        List servicoList = this.servicosRepository.findAll(sort);
        return servicoList;
    }

    public String deleteService(Long id) {
        Optional servicoOptional = this.servicosRepository.findById(id);
        if (!servicoOptional.isPresent()) {
            return null;
        }
        this.servicosRepository.deleteById(((Servico)servicoOptional.get()).getId());
        return "ok";
    }

    public List<Servico> loadService(Map<String, Servico> requisicaoNovoServicoList) {
        ArrayList<Servico> list = new ArrayList<Servico>(requisicaoNovoServicoList.size());
        for (Servico ls : requisicaoNovoServicoList.values()) {
            RequisicaoNovoServico lol = new RequisicaoNovoServico();
            BeanUtils.copyProperties((Object)ls, (Object)lol);
            list.add(ls);
        }
        this.servicosRepository.saveAll(list);
        return list;
    }

    public void editActivityServices(List<String> aliasServices) {
        List<Servico> servicoList = this.servicosRepository.findByAliasIn(aliasServices);
        servicoList.forEach(servico -> servico.setActivity(!servico.isActivity()));
        this.servicosRepository.saveAll(servicoList);
    }

    public void editPriceService(Long id, BigDecimal newPrice) {
        Servico servico = (Servico)this.servicosRepository.findById(id).get();
        servico.setPrice(newPrice);
        this.servicosRepository.save(servico);
    }

    public void subtractQuantityFor0All() {
        List<br.com.store24h.store24h.model.Servico> servicoList = this.servicosRepository.findAll();
        servicoList.forEach(servico -> servico.setTotalQuantity(0));
        this.servicosRepository.saveAll(servicoList);
        this.chipService.reset();
    }

    public void subtractQuantity(Servico service) {
        service.setTotalQuantity(service.getTotalQuantity() - 1 < 0 ? 0 : service.getTotalQuantity() - 1);
        this.servicosRepository.save(service);
    }

    public void addQuantity(String serviceName) {
        Servico service = this.servicosRepository.findByName(serviceName).get();
        service.setTotalQuantity(service.getTotalQuantity() + 1);
        this.servicosRepository.save(service);
    }

    public void addQuantityAllService(List<Servico> servicoList) {
        servicoList.forEach(service -> service.setTotalQuantity(service.getTotalQuantity() + 1));
        this.servicosRepository.saveAll(servicoList);
    }

    public void saveRegisterBuy(Long idActivation, Servico servico, String number, Long idUser) {
        CompraServiso compraServiso = new CompraServiso(idActivation, servico.getAlias(), number, servico.getPrice(), idUser);
        CompraServiso salvo = (CompraServiso)this.buyServiceRepository.save(compraServiso);
        this.cacheService.setCompraServicoByActivation(idActivation.toString(), salvo.getId().toString(), Duration.ofHours(24L));
    }
}
