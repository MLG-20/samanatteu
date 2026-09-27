package com.samanatteu.service.cotisation;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.entity.Tirage;
import com.samanatteu.exception.TirageDejaExistantPourCeCycleException;
import com.samanatteu.repository.TirageRepository;

@Service
public class TirageService {
    private final TirageRepository tirageRepository;

    public TirageService(TirageRepository tirageRepository) {
        this.tirageRepository = tirageRepository;
    }

    public List<TirageDTO> listTirage() {
        return tirageRepository.findAll()
                .stream()
                .map(this::convertiTirageDTO)
                .toList();
    }

    public TirageDTO createTirage(Tirage tirage) {
        if (tirageRepository.existsByCycleId(tirage.getCycle().getId())) {
            throw new TirageDejaExistantPourCeCycleException(tirage.getCycle().getId());
        }
        Tirage enregistre = tirageRepository.save(tirage);
        return convertiTirageDTO(enregistre);
    }

    public Optional<TirageDTO> updateTirage(Long id, Tirage tirageModifier) {
        return tirageRepository.findById(id).map(tirageExsitant -> {
            tirageExsitant.setCycle(tirageModifier.getCycle());
            tirageExsitant.setParticipation(tirageModifier.getParticipation());
            tirageExsitant.setMontantGagne(tirageModifier.getMontantGagne());
            tirageExsitant.setDateTirage(tirageModifier.getDateTirage());
            tirageExsitant.setDateVersement(tirageModifier.getDateVersement());
            tirageExsitant.setStatut(tirageModifier.getStatut());
            tirageExsitant.setCreatedAt(tirageModifier.getCreatedAt());

            Tirage enregistre = tirageRepository.save(tirageExsitant);
            return convertiTirageDTO(enregistre);
        });
    }

    public boolean deleteTirage(Long id) {
        if (tirageRepository.existsById(id)) {
            tirageRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private TirageDTO convertiTirageDTO(Tirage tirage) {
        TirageDTO dto = new TirageDTO();
        dto.setId(tirage.getId());
        dto.setCycleId(tirage.getCycle().getId());
        dto.setParticipationId(tirage.getParticipation().getId());
        dto.setMontantGagne(tirage.getMontantGagne());
        dto.setDateTirage(tirage.getDateTirage());
        dto.setDateVersement(tirage.getDateVersement());
        dto.setStatut(tirage.getStatut());
        dto.setCreatedAt(tirage.getCreatedAt());

        return dto;
    }
}
