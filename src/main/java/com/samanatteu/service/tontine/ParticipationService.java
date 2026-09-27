package com.samanatteu.service.tontine;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.enums.StatutParticipation;
import com.samanatteu.enums.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.InscriptionsFermeesException;
import com.samanatteu.exception.MembreIntrouvableException;
import com.samanatteu.exception.NombrePartsInvalideException;
import com.samanatteu.exception.ParticipationDejaExistanteException;
import com.samanatteu.exception.RelationObligatoireException;
import com.samanatteu.exception.TontineIntrouvableException;
import com.samanatteu.repository.ParticipationRepository;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;
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

    // Lister
    public List<ParticipationDTO> listParticipation() {

        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Participation> participations = estGestionnaire
                ? participationRepository.findByTontineGestionnaireTelephone(utilisateurConnecte.telephone())
                : participationRepository.findByMembreTelephone(utilisateurConnecte.telephone());
        return participations.stream()
                .map(this::convertiParticipationDTO) // 3. applique la conversion à CHAQUE Participation ->
                                                     // ParticipationDTO
                                                     // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<ParticipationDTO> à partir du flux
    }

    // créer : inscrire un membre à une tontine.
    // Les vérifications sont dans un ORDRE VOLONTAIRE : on contrôle d'abord "qui a
    // le droit"
    // (propriétaire), ensuite "est-ce possible" (doublon, statut, parts). Ainsi un
    // étranger
    // reçoit un 403 avant d'apprendre quoi que ce soit sur la tontine.
    public ParticipationDTO createParticipation(Participation participation) {
        // 1. Le client doit avoir indiqué un membre et une tontine (sinon 400).
        if (participation.getMembre() == null || participation.getMembre().getId() == null) {
            throw new RelationObligatoireException("membre");
        }
        if (participation.getTontine() == null || participation.getTontine().getId() == null) {
            throw new RelationObligatoireException("tontine");
        }
        // 2. On NE FAIT PAS confiance à la tontine du JSON (ce n'est qu'un {id: 5}
        // fabriqué
        // par le client) : on va chercher la vraie tontine en base. Introuvable -> 404.
        Tontine tontine = tontineRepository.findById(participation.getTontine().getId())
                .orElseThrow(() -> new TontineIntrouvableException());
        // 3. Seul le gestionnaire de CETTE tontine peut y inscrire quelqu'un (sinon
        // 403).
        verifierProprietaire(tontine);

        // 3b. Même principe que pour la tontine : le membre du JSON n'est qu'un {id:
        // 30}
        // fabriqué par le client. On va chercher le vrai membre en base. S'il n'existe
        // pas
        // -> 404 propre (sinon la base refuserait l'enregistrement et le client
        // recevrait un 500).
        Utilisateur membre = utilisateurRepository.findById(participation.getMembre().getId())
                .orElseThrow(() -> new MembreIntrouvableException());
        // 4. Pas de doublon : ce membre ne doit pas déjà être inscrit à cette tontine
        // (409).
        if (participationRepository.existsByMembreIdAndTontineId(
                participation.getMembre().getId(), tontine.getId())) {
            throw new ParticipationDejaExistanteException();
        }
        // 5. On n'inscrit que tant que la tontine est EN_ATTENTE : ajouter un
        // participant (ou des
        // parts) après le début fausserait le tirage (409). On lit le statut EN BASE.
        verifierInscriptionsOuvertes(tontine);
        // 6. Le nombre de parts est obligatoire et au moins 1 (le tirage est pondéré
        // par les parts).
        // Le test "== null" vient EN PREMIER : "null < 1" ferait planter
        // (NullPointerException).
        verifierNombreParts(participation.getNombreParts());
        // 7. Ces trois champs sont décidés par le SERVEUR, jamais par le client :
        // - statut : toujours ACTIF à l'inscription ;
        // - date d'adhésion : la date du jour ;
        // - ordre d'inscription : (plus grand ordre déjà pris dans cette tontine) + 1,
        // ou 1 si personne n'est encore inscrit. On prend le "dernier + 1" et non "le
        // nombre
        // d'inscrits + 1", car après une suppression au milieu, le second donnerait un
        // numéro déjà pris.
        participation.setStatut(StatutParticipation.ACTIF);
        participation.setDateAdhesion(Date.valueOf(LocalDate.now()));
        participation.setOrdreInscription(
                participationRepository.findFirstByTontineIdOrderByOrdreInscriptionDesc(tontine.getId())
                        .map(derniere -> derniere.getOrdreInscription() + 1)
                        .orElse(1));
        // 8. On remplace la tontine "fabriquée" du JSON par la vraie, chargée en base à
        // l'étape 2.
        participation.setTontine(tontine);
        participation.setMembre(membre);
        Participation enregistree = participationRepository.save(participation);
        return convertiParticipationDTO(enregistree);
    }

    // update
    public Optional<ParticipationDTO> updateParticipation(Long id, Participation participationModifier) {
        // findById(id) renvoie un Optional<Participation> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return participationRepository.findById(id).map(existante -> {
            verifierProprietaire(existante.getTontine());
            verifierInscriptionsOuvertes(existante.getTontine());
            verifierNombreParts(participationModifier.getNombreParts());
            existante.setNombreParts(participationModifier.getNombreParts());
            return convertiParticipationDTO(participationRepository.save(existante));
        });
    }

    // Delete
    public boolean deleteParticipation(Long id) {
        Optional<Participation> participation = participationRepository.findById(id);
        if (participation.isPresent()) {
            verifierProprietaire(participation.get().getTontine());
            verifierInscriptionsOuvertes(participation.get().getTontine());
            participationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private void verifierProprietaire(Tontine tontine) {
        if (!tontine.estGereePar(utilisateurConnecte.telephone())) {
            throw new AccesRefuseException();
        }
    }

    private void verifierInscriptionsOuvertes(Tontine tontine) {
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }
    }

    private void verifierNombreParts(Integer nombreParts) {
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
