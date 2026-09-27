package com.samanatteu.service.pret;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.entity.Pret;
import com.samanatteu.enums.StatutPret;
import com.samanatteu.exception.PretDejaEnCoursException;
import com.samanatteu.repository.PretRepository;

@Service
public class PretService {
    private final PretRepository pretRepository;

    public PretService(PretRepository pretRepository) {
        this.pretRepository = pretRepository;
    }

    public List<PretDTO> listPret() {
        return pretRepository.findAll()
                .stream()
                .map(this::convertiPretDTO)
                .toList();
    }

    public PretDTO createPret(Pret pret) {
        if (pretRepository.existsByMembreIdAndStatut(pret.getMembre().getId(),StatutPret.ACTIF)) {
            throw new PretDejaEnCoursException(pret.getMembre().getId());
        }
        Pret enregistre = pretRepository.save(pret);
        return convertiPretDTO(enregistre);
    }

    public Optional<PretDTO> updatePret(Long id, Pret pretModifier) {
        return pretRepository.findById(id).map(pretExsitant -> {
            pretExsitant.setMembre(pretModifier.getMembre());
            pretExsitant.setTontine(pretModifier.getTontine());
            pretExsitant.setGestionnaire(pretModifier.getGestionnaire());
            pretExsitant.setMontant(pretModifier.getMontant());
            pretExsitant.setTauxInteret(pretModifier.getTauxInteret());
            pretExsitant.setNbEcheances(pretModifier.getNbEcheances());
            pretExsitant.setDateAccord(pretModifier.getDateAccord());
            pretExsitant.setStatut(pretModifier.getStatut());
            pretExsitant.setCreatedAt(pretModifier.getCreatedAt());

            Pret enregistre = pretRepository.save(pretExsitant);
            return convertiPretDTO(enregistre);
        });
    }

    public boolean deletePret(Long id) {
        if (pretRepository.existsById(id)) {
            pretRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PretDTO convertiPretDTO(Pret pret) {
        PretDTO dto = new PretDTO();
        dto.setId(pret.getId());
        dto.setMembreId(pret.getMembre().getId());
        dto.setTontineId(pret.getTontine().getId());
        dto.setGestionnaireId(pret.getGestionnaire().getId());
        dto.setMontant(pret.getMontant());
        dto.setTauxInteret(pret.getTauxInteret());
        dto.setNbEcheances(pret.getNbEcheances());
        dto.setDateAccord(pret.getDateAccord());
        dto.setStatut(pret.getStatut());
        dto.setCreatedAt(pret.getCreatedAt());

        return dto;
    }
}
