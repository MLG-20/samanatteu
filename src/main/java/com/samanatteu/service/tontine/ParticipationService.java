package com.samanatteu.service.tontine;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.NombrePartsInvalideException;
import com.samanatteu.exception.tontine.ParticipationDejaExistanteException;
import com.samanatteu.exception.tontine.RelationObligatoireException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.utilisateur.MembreIntrouvableException;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final TontineRepository tontineRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public ParticipationService(ParticipationRepository participationRepository,
            TontineRepository tontineRepository, UtilisateurRepository utilisateurRepository,
            UtilisateurConnecte utilisateurConnecte) {
        this.participationRepository = participationRepository;
        this.tontineRepository = tontineRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    public List<ParticipationDTO> listParticipation() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Participation> participations = estGestionnaire
                ? participationRepository.findByTontineGestionnaireTelephone(utilisateurConnecte.telephone())
                : participationRepository.findByMembreTelephone(utilisateurConnecte.telephone());
        return participations.stream()
                .map(this::convertiParticipationDTO)
                .toList();
    }

    // Ordre volontaire des contrôles : d'abord « qui a le droit » (propriétaire), ensuite
    // « est-ce possible » (membre, doublon, statut, parts). Un étranger reçoit ainsi un 403
    // avant d'apprendre quoi que ce soit sur la tontine.
    public ParticipationDTO createParticipation(Participation participation) {
        if (participation.getMembre() == null || participation.getMembre().getId() == null) {
            throw new RelationObligatoireException("membre");
        }
        if (participation.getTontine() == null || participation.getTontine().getId() == null) {
            throw new RelationObligatoireException("tontine");
        }
        // La tontine et le membre du JSON ne sont que des {id: …} fabriqués par le client :
        // on recharge les vrais en base (404 propre au lieu d'un 500 sur la clé étrangère).
        Tontine tontine = tontineRepository.findById(participation.getTontine().getId())
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);

        Utilisateur membre = utilisateurRepository.findById(participation.getMembre().getId())
                .orElseThrow(() -> new MembreIntrouvableException());
        if (participationRepository.existsByMembreIdAndTontineId(
                participation.getMembre().getId(), tontine.getId())) {
            throw new ParticipationDejaExistanteException();
        }
        // Ajouter un participant (ou des parts) après le démarrage fausserait le tirage.
        verifierInscriptionsOuvertes(tontine);
        verifierNombreParts(participation.getNombreParts());

        // Statut, date d'adhésion et ordre d'inscription sont décidés par le serveur.
        // Ordre = dernier + 1 (et non nombre d'inscrits + 1, qui redonnerait un numéro
        // déjà pris après une suppression au milieu).
        participation.setStatut(StatutParticipation.ACTIF);
        participation.setDateAdhesion(Date.valueOf(LocalDate.now()));
        participation.setOrdreInscription(
                participationRepository.findFirstByTontineIdOrderByOrdreInscriptionDesc(tontine.getId())
                        .map(derniere -> derniere.getOrdreInscription() + 1)
                        .orElse(1));
        participation.setTontine(tontine);
        participation.setMembre(membre);
        Participation enregistree = participationRepository.save(participation);
        return convertiParticipationDTO(enregistree);
    }

    public Optional<ParticipationDTO> updateParticipation(Long id, Participation participationModifier) {
        return participationRepository.findById(id).map(existante -> {
            utilisateurConnecte.verifierGestionnaire(existante.getTontine());
            verifierInscriptionsOuvertes(existante.getTontine());
            verifierNombreParts(participationModifier.getNombreParts());
            existante.setNombreParts(participationModifier.getNombreParts());
            return convertiParticipationDTO(participationRepository.save(existante));
        });
    }

    public boolean deleteParticipation(Long id) {
        Optional<Participation> participation = participationRepository.findById(id);
        if (participation.isPresent()) {
            utilisateurConnecte.verifierGestionnaire(participation.get().getTontine());
            verifierInscriptionsOuvertes(participation.get().getTontine());
            participationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void verifierInscriptionsOuvertes(Tontine tontine) {
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }
    }

    private void verifierNombreParts(Integer nombreParts) {
        // null testé en premier : "null < 1" lèverait une NullPointerException.
        if (nombreParts == null || nombreParts < 1) {
            throw new NombrePartsInvalideException();
        }
    }

    private ParticipationDTO convertiParticipationDTO(Participation Participation) {
        ParticipationDTO dto = new ParticipationDTO();
        dto.setId(Participation.getId());
        dto.setMembreId(Participation.getMembre().getId());
        dto.setTontineId(Participation.getTontine().getId());
        dto.setNombreParts(Participation.getNombreParts());
        dto.setDateAdhesion(Participation.getDateAdhesion());
        dto.setStatut(Participation.getStatut());
        dto.setOrdreInscription(Participation.getOrdreInscription());

        return dto;
    }
}
