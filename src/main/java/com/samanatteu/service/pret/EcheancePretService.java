package com.samanatteu.service.pret;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.EcheancePretDTO;
import com.samanatteu.entity.EcheancePret;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.exception.NumeroEcheanceDejaExistantException;
import com.samanatteu.repository.EcheancePretRepository;

@Service 
public class EcheancePretService {
     private final EcheancePretRepository echeancePretRepository;

    public EcheancePretService(EcheancePretRepository echeancePretRepository) {
        this.echeancePretRepository = echeancePretRepository;
    }

    public List<EcheancePretDTO> listEcheancePret() {
        return echeancePretRepository.findAll()
                .stream()
                .map(this::convertiEcheancePretDTO)
                .toList();
    }

    public EcheancePretDTO createEcheancePret(EcheancePret echeancePret) {
        if (echeancePretRepository.existsByNumeroEcheanceAndPretId(echeancePret.getNumeroEcheance(), echeancePret.getPret().getId())) {
            throw new NumeroEcheanceDejaExistantException(echeancePret.getNumeroEcheance(), echeancePret.getPret().getId());
        }
        if (echeancePret.getMontantPaye().compareTo(echeancePret.getMontantDu()) > 0) {
            throw new MontantPayeSuperieurAuDuException(echeancePret.getMontantPaye(), echeancePret.getMontantDu());
        }
        EcheancePret enregistre = echeancePretRepository.save(echeancePret);
        return convertiEcheancePretDTO(enregistre);
    }

    public Optional<EcheancePretDTO> updateEcheancePret(Long id, EcheancePret echeancePretModifier) {
        return echeancePretRepository.findById(id).map(echeancePretExsitante -> {
            echeancePretExsitante.setPret(echeancePretModifier.getPret());
            echeancePretExsitante.setNumeroEcheance(echeancePretModifier.getNumeroEcheance());
            echeancePretExsitante.setMontantDu(echeancePretModifier.getMontantDu());
            echeancePretExsitante.setMontantPaye(echeancePretModifier.getMontantPaye());
            echeancePretExsitante.setDateEcheance(echeancePretModifier.getDateEcheance());
            echeancePretExsitante.setDatePaiement(echeancePretModifier.getDatePaiement());
            echeancePretExsitante.setStatut(echeancePretModifier.getStatut());

            EcheancePret enregistre = echeancePretRepository.save(echeancePretExsitante);
            return convertiEcheancePretDTO(enregistre);
        });
    }

    public boolean deleteEcheancePret(Long id) {
        if (echeancePretRepository.existsById(id)) {
            echeancePretRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private EcheancePretDTO convertiEcheancePretDTO(EcheancePret echeancePret) {
        EcheancePretDTO dto = new EcheancePretDTO();
        dto.setId(echeancePret.getId());
        dto.setPretId(echeancePret.getPret().getId());
        dto.setNumeroEcheance(echeancePret.getNumeroEcheance());
        dto.setMontantDu(echeancePret.getMontantDu());
        dto.setMontantPaye(echeancePret.getMontantPaye());
        dto.setDateEcheance(echeancePret.getDateEcheance());
        dto.setDatePaiement(echeancePret.getDatePaiement());
        dto.setStatut(echeancePret.getStatut());

        return dto;
    }
}
