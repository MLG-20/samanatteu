package com.samanatteu.service.tontine;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.tontine.TontineNonModifiableException;
import com.samanatteu.exception.tontine.TontineSansMembreException;
import com.samanatteu.exception.tontine.TransitionStatutInvalideException;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
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
        // 0 = « pas encore calculé » (colonne NOT NULL) : nbCycles se déduit
        // des parts des membres, calculé par le serveur à l'activation.
        // Écrase ce que le client aurait envoyé dans le JSON.
        tontine.setNbCycles(0);
        // Caisse de prêts vide au départ. Obligatoire : Hibernate écrit NULL
        // si le champ est null (le DEFAULT 0 de la base ne joue pas), et
        // écrase un solde que le client aurait glissé dans le JSON.
        tontine.setSoldeCaissePret(BigDecimal.ZERO);
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
            // montantCaissePret = une RÈGLE choisie par la gestionnaire :
            // recopiée. soldeCaissePret = un ÉTAT calculé par le serveur :
            // jamais recopié du JSON (sinon on prêterait de l'argent fictif).
            tontineExsitant.setMontantCaissePret(tontineModifier.getMontantCaissePret());
            tontineExsitant.setFrequence(tontineModifier.getFrequence());
            tontineExsitant.setIntervalle(tontineModifier.getIntervalle());
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

            // 1re activation seulement (EN_ATTENTE → ACTIVE) : nbCycles se
            // fixe UNE fois au lancement. Une reprise (SUSPENDUE → ACTIVE) ne
            // recalcule pas : des cycles ont déjà eu lieu. Placé APRÈS le
            // contrôle de transition et AVANT le save (sinon non enregistré).
            if (tontine.getStatut() == StatutTontine.EN_ATTENTE && nouveau == StatutTontine.ACTIVE) {
                // Modèle B : une part = un gain = un cycle, donc
                // nbCycles = somme des parts des membres ACTIF.
                // mapToInt donne un flux de nombres (IntStream) qui sait
                // faire sum() ; map donnerait des Integer, sans sum().
                int totalParts = participationRepository
                        .findByTontineIdAndStatut(tontine.getId(), StatutParticipation.ACTIF)
                        .stream()
                        .mapToInt(Participation::getNombreParts)
                        .sum();
                // Refus au plus tôt, là où est la vraie cause : sinon
                // ouvrirCycle échouerait plus tard avec « 0 cycles prévus ».
                if (totalParts == 0) {
                    throw new TontineSansMembreException();
                }
                tontine.setNbCycles(totalParts);
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
        dto.setMontantCaissePret(tontine.getMontantCaissePret());
        dto.setSoldeCaissePret(tontine.getSoldeCaissePret());
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
