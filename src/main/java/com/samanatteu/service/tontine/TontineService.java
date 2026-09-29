package com.samanatteu.service.tontine;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.enums.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.TontineNonModifiableException;
import com.samanatteu.exception.TransitionStatutInvalideException;
import com.samanatteu.repository.ParticipationRepository;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class TontineService {
    private final TontineRepository tontineRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParticipationRepository participationRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public TontineService(TontineRepository tontineRepository, UtilisateurRepository utilisateurRepository,
            ParticipationRepository participationRepository, UtilisateurConnecte utilisateurConnecte) {
        this.tontineRepository = tontineRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.participationRepository = participationRepository;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    public List<TontineDTO> listTontine() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Tontine> tontines = estGestionnaire
                ? tontineRepository.findByGestionnaireTelephone(utilisateurConnecte.telephone())
                : participationRepository.findByMembreTelephone(utilisateurConnecte.telephone())
                        .stream()
                        .map(Participation::getTontine)
                        .toList();

        return tontines.stream()
                .map(this::convertiTontineDTO)
                .toList();
    }

    public TontineDTO createTontine(Tontine tontine) {
        Utilisateur gestionnaire = utilisateurRepository.findByTelephone(utilisateurConnecte.telephone())
                .orElseThrow(AccesRefuseException::new);
        tontine.setGestionnaire(gestionnaire);
        tontine.setStatut(StatutTontine.EN_ATTENTE);
        Tontine enregistre = tontineRepository.save(tontine);
        return convertiTontineDTO(enregistre);
    }

    public Optional<TontineDTO> updateTontine(Long id, Tontine tontineModifier) {
        return tontineRepository.findById(id).map(tontineExsitant -> {
            utilisateurConnecte.verifierGestionnaire(tontineExsitant);

            if (tontineExsitant.getStatut() != StatutTontine.EN_ATTENTE) {
                throw new TontineNonModifiableException(tontineExsitant.getStatut().toString());
            }
            tontineExsitant.setNom(tontineModifier.getNom());
            tontineExsitant.setMontantPart(tontineModifier.getMontantPart());
            tontineExsitant.setFrequence(tontineModifier.getFrequence());
            tontineExsitant.setIntervalle(tontineModifier.getIntervalle());
            tontineExsitant.setNbCycles(tontineModifier.getNbCycles());
            tontineExsitant.setDescription(tontineModifier.getDescription());
            tontineExsitant.setJourCotisation(tontineModifier.getJourCotisation());
            Tontine enregistre = tontineRepository.save(tontineExsitant);
            return convertiTontineDTO(enregistre);
        });
    }

    public boolean deleteTontine(Long id) {
        Optional<Tontine> tontine = tontineRepository.findById(id);
        if (tontine.isPresent()) {
            utilisateurConnecte.verifierGestionnaire(tontine.get());
            tontineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<TontineDTO> activerTontine(Long id) {
        return changerStatut(id, StatutTontine.ACTIVE, StatutTontine.EN_ATTENTE, StatutTontine.SUSPENDUE);
    }

    public Optional<TontineDTO> suspendreTontine(Long id) {
        return changerStatut(id, StatutTontine.SUSPENDUE, StatutTontine.ACTIVE);
    }

    public Optional<TontineDTO> cloturerTontine(Long id) {
        return changerStatut(id, StatutTontine.TERMINEE, StatutTontine.ACTIVE);
    }

    private Optional<TontineDTO> changerStatut(Long id, StatutTontine nouveau, StatutTontine... autorises) {
        return tontineRepository.findById(id).map(tontine -> {
            utilisateurConnecte.verifierGestionnaire(tontine);
            if (!Arrays.asList(autorises).contains(tontine.getStatut())) {
                throw new TransitionStatutInvalideException(tontine.getStatut(), nouveau);
            }
            tontine.setStatut(nouveau);
            return convertiTontineDTO(tontineRepository.save(tontine));
        });
    }

    private TontineDTO convertiTontineDTO(Tontine tontine) {
        TontineDTO dto = new TontineDTO();
        dto.setId(tontine.getId());
        dto.setNom(tontine.getNom());
        dto.setMontantPart(tontine.getMontantPart());
        dto.setFrequence(tontine.getFrequence());
        dto.setIntervalle(tontine.getIntervalle());
        dto.setNbCycles(tontine.getNbCycles());
        dto.setDescription(tontine.getDescription());
        dto.setJourCotisation(tontine.getJourCotisation());
        dto.setCreatedAt(tontine.getCreatedAt());
        dto.setUpdatedAt(tontine.getUpdatedAt());
        dto.setStatut(tontine.getStatut());
        dto.setGestionnaireId(tontine.getGestionnaire().getId());

        return dto;
    }
}
