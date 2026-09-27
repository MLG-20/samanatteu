package com.samanatteu.service.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.entity.Cycle;
import com.samanatteu.exception.NumeroCycleDejaExistantException;
import com.samanatteu.repository.CycleRepository;

@Service 
public class CycleService {
    private final CycleRepository cycleRepository;

    public CycleService(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    public List<CycleDTO> listCycle() {
        return cycleRepository.findAll()
                .stream()
                .map(this::convertiCycleDTO)
                .toList();
    }

    public CycleDTO createCycle(Cycle cycle) {
        if (cycleRepository.existsByNumeroCycleAndTontineId(cycle.getNumeroCycle(), cycle.getTontine().getId())) {
            throw new NumeroCycleDejaExistantException(cycle.getNumeroCycle(), cycle.getTontine().getId());
        }
        Cycle enregistre = cycleRepository.save(cycle);
        return convertiCycleDTO(enregistre);
    }

    public Optional<CycleDTO> updateCycle(Long id, Cycle cycleModifier) {
        return cycleRepository.findById(id).map(cycleExsitant -> {
            cycleExsitant.setTontine(cycleModifier.getTontine());
            cycleExsitant.setNumeroCycle(cycleModifier.getNumeroCycle());
            cycleExsitant.setDateDebut(cycleModifier.getDateDebut());
            cycleExsitant.setDateFinPrevue(cycleModifier.getDateFinPrevue());
            cycleExsitant.setDateFinReelle(cycleModifier.getDateFinReelle());
            cycleExsitant.setMontantAttendu(cycleModifier.getMontantAttendu());
            cycleExsitant.setMontantCollecte(cycleModifier.getMontantCollecte());
            cycleExsitant.setStatut(cycleModifier.getStatut());
            cycleExsitant.setCreatedAt(cycleModifier.getCreatedAt());

            Cycle enregistre = cycleRepository.save(cycleExsitant);
            return convertiCycleDTO(enregistre);
        });
    }

    public boolean deleteCycle(Long id) {
        if (cycleRepository.existsById(id)) {
            cycleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CycleDTO convertiCycleDTO(Cycle cycle) {
        CycleDTO dto = new CycleDTO();
        dto.setId(cycle.getId());
        dto.setTontineId(cycle.getTontine().getId());
        dto.setNumeroCycle(cycle.getNumeroCycle());
        dto.setDateDebut(cycle.getDateDebut());
        dto.setDateFinPrevue(cycle.getDateFinPrevue());
        dto.setDateFinReelle(cycle.getDateFinReelle());
        dto.setMontantAttendu(cycle.getMontantAttendu());
        dto.setMontantCollecte(cycle.getMontantCollecte());
        dto.setStatut(cycle.getStatut());
        dto.setCreatedAt(cycle.getCreatedAt());

        return dto;
    }
}
