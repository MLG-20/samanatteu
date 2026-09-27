package com.samanatteu.service.cotisation;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.entity.Cotisation;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.repository.CotisationRepository;

@Service
public class CotisationService {
    private final CotisationRepository cotisationRepository;

    public CotisationService(CotisationRepository cotisationRepository) {
        this.cotisationRepository = cotisationRepository;
    }

    public List<CotisationDTO> listCotisations() {
        return cotisationRepository.findAll()
                .stream()
                .map(this::convertiCotisationDTO)
                .toList();
    }

    public CotisationDTO createCotisation(Cotisation cotisation) {
        if (cotisation.getMontantPaye().compareTo(cotisation.getMontantDu()) > 0){
            throw new MontantPayeSuperieurAuDuException(cotisation.getMontantPaye(), cotisation.getMontantDu());
        }
        Cotisation enregistre = cotisationRepository.save(cotisation);
        return convertiCotisationDTO(enregistre);
    }

    public Optional<CotisationDTO> updateCotisation(Long id, Cotisation cotisationModifier) {
        return cotisationRepository.findById(id).map(cotisationExsitante -> {
            cotisationExsitante.setParticipation(cotisationModifier.getParticipation());
            cotisationExsitante.setCycle(cotisationModifier.getCycle());
            cotisationExsitante.setMontantDu(cotisationModifier.getMontantDu());
            cotisationExsitante.setMontantPaye(cotisationModifier.getMontantPaye());
            cotisationExsitante.setDatePaiement(cotisationModifier.getDatePaiement());
            cotisationExsitante.setModePaiement(cotisationModifier.getModePaiement());
            cotisationExsitante.setReference(cotisationModifier.getReference());
            cotisationExsitante.setStatut(cotisationModifier.getStatut());
            cotisationExsitante.setCreatedAt(cotisationModifier.getCreatedAt());

            Cotisation enregistre = cotisationRepository.save(cotisationExsitante);
            return convertiCotisationDTO(enregistre);
        });
    }

    public boolean deleteCotisation(Long id) {
        if (cotisationRepository.existsById(id)) {
            cotisationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private CotisationDTO convertiCotisationDTO(Cotisation cotisation) {
        CotisationDTO dto = new CotisationDTO();
        dto.setId(cotisation.getId());
        dto.setParticipationId(cotisation.getParticipation().getId());
        dto.setCycleId(cotisation.getCycle().getId());
        dto.setMontantDu(cotisation.getMontantDu());
        dto.setMontantPaye(cotisation.getMontantPaye());
        dto.setDatePaiement(cotisation.getDatePaiement());
        dto.setModePaiement(cotisation.getModePaiement());
        dto.setReference(cotisation.getReference());
        dto.setStatut(cotisation.getStatut());
        dto.setCreatedAt(cotisation.getCreatedAt());

        return dto;
    }
}
