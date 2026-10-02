package com.samanatteu.service.onboarding;

import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.samanatteu.dto.onboarding.DemandeInvitationDTO;
import com.samanatteu.dto.onboarding.InvitationDTO;
import com.samanatteu.dto.onboarding.LienInvitationDTO;
import com.samanatteu.entity.onboarding.Invitation;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.onboarding.StatutInvitation;
import com.samanatteu.enums.onboarding.TypeInvitation;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.onboarding.InvitationAnnuleeException;
import com.samanatteu.exception.onboarding.InvitationDejaUtiliseeException;
import com.samanatteu.exception.onboarding.InvitationExpireeException;
import com.samanatteu.exception.onboarding.InvitationIntrouvableException;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.ParticipationDejaExistanteException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.repository.onboarding.InvitationRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.notification.NotificationService;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final TontineRepository tontineRepository;
    private final UtilisateurConnecte utilisateurConnecte;
    private final ParticipationRepository participationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    // SecureRandom et pas Random : un token d'invitation donne accès à une
    // tontine, il ne doit pas pouvoir être prédit (même raison que le tirage).
    private final SecureRandom hasard = new SecureRandom();

    public InvitationService(InvitationRepository invitationRepository, TontineRepository tontineRepository,
            UtilisateurConnecte utilisateurConnect, ParticipationRepository participationRepository,
            UtilisateurRepository utilisateurRepository, NotificationService notificationService) {
        this.invitationRepository = invitationRepository;
        this.tontineRepository = tontineRepository;
        this.utilisateurConnecte = utilisateurConnect;
        this.participationRepository = participationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;

    }

    // Filtrée par gestionnaire : les tokens sont des clés d'accès, jamais visibles d'un autre.
    public List<InvitationDTO> listInvitation() {
        return invitationRepository
                .findByTontineGestionnaireTelephoneOrderByCreatedAtDesc(utilisateurConnecte.telephone())
                .stream()
                .map(this::convertiInvitationDTO)
                .toList();
    }

    // Tout ou rien : annuler l'ancien lien et créer le nouveau forment un seul bloc
    // (sinon un échec au milieu laisserait la tontine sans aucun lien valide).
    @Transactional
    public InvitationDTO genererLienGroupe(Long tontineId) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);
        // Même règle que les participations : pas de nouveau membre après le démarrage.
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }

        // Option A : un seul lien de groupe actif. L'ancien est annulé, pas supprimé
        // (si quelqu'un clique dessus, on peut lui dire qu'il a été remplacé).
        List<Invitation> anciens = invitationRepository.findByTontineIdAndTypeAndStatut(
                tontineId, TypeInvitation.GROUPE, StatutInvitation.EN_ATTENTE);
        for (Invitation ancien : anciens) {
            ancien.setStatut(StatutInvitation.ANNULEE);
        }
        invitationRepository.saveAll(anciens);

        // Tout est fixé par le serveur : le client n'envoie que l'id de la tontine.
        Invitation lien = new Invitation();
        lien.setTontine(tontine);
        lien.setType(TypeInvitation.GROUPE);
        lien.setToken(genererToken());
        lien.setStatut(StatutInvitation.EN_ATTENTE);
        lien.setCreatedAt(LocalDateTime.now());
        // Calculé depuis createdAt : exactement 7 jours d'écart (CDC §4.7).
        lien.setExpireAt(lien.getCreatedAt().plusDays(7));

        return convertiInvitationDTO(invitationRepository.save(lien));

    }

    public InvitationDTO genererInvitationIndividuelle(Long tontineId, DemandeInvitationDTO demande) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new TontineIntrouvableException());
        utilisateurConnecte.verifierGestionnaire(tontine);
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }

        Invitation invitation = new Invitation();
        invitation.setTontine(tontine);
        invitation.setType(TypeInvitation.INDIVIDUELLE);
        invitation.setToken(genererToken());
        invitation.setStatut(StatutInvitation.EN_ATTENTE);
        invitation.setTelephone(demande.getTelephone());
        invitation.setPrenomPreRempli(demande.getPrenom());
        invitation.setNomPreRempli(demande.getNom());
        // Parts fixées dès l'invitation (gnari lokho possible) ; 1 si non précisé.
        invitation.setNombreParts(demande.getNombreParts() != null ? demande.getNombreParts() : 1);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpireAt(invitation.getCreatedAt().plusDays(7));
        return convertiInvitationDTO(invitationRepository.save(invitation));
    }

    // Appelée dès le clic : dit tout de suite si le lien est utilisable (sinon
    // message clair : remplacé, expiré, déjà utilisé, inscriptions closes).
    public LienInvitationDTO consulterLien(String token) {
        Invitation invitation = trouverInvitationValide(token);
        LienInvitationDTO dto = new LienInvitationDTO();
        dto.setNomTontine(invitation.getTontine().getNom());
        dto.setType(invitation.getType());
        dto.setPrenomPreRempli(invitation.getPrenomPreRempli());
        dto.setNomPreRempli(invitation.getNomPreRempli());
        dto.setTelephone(invitation.getTelephone());
        dto.setExpireAt(invitation.getExpireAt());

        return dto;
    }

    // Contrôles communs à la consultation et à l'acceptation d'un lien.
    // Expiration jugée sur la date, pas sur le statut EXPIRE (jamais écrit par personne).
    private Invitation trouverInvitationValide(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationIntrouvableException());
        if (invitation.getStatut() == StatutInvitation.ANNULEE) {
            throw new InvitationAnnuleeException();
        }
        if (invitation.getStatut() == StatutInvitation.ACCEPTE) {
            throw new InvitationDejaUtiliseeException();
        }
        if (invitation.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new InvitationExpireeException();
        }
        // Lien encore valide mais tontine démarrée entre-temps : inscriptions closes.
        Tontine tontine = invitation.getTontine();
        if (tontine.getStatut() != StatutTontine.EN_ATTENTE) {
            throw new InscriptionsFermeesException(tontine.getStatut().toString());
        }

        return invitation;
    }

    // Tout ou rien : la participation et le passage en ACCEPTE vont ensemble
    // (sinon une invitation à usage unique pourrait servir deux fois).
    @Transactional
    public void rejoindre(String token) {
        Invitation invitation = trouverInvitationValide(token);
        Tontine tontine = invitation.getTontine();
        Utilisateur membre = utilisateurRepository.findByTelephone(utilisateurConnecte.telephone())
                .orElseThrow(() -> new AccesRefuseException());
        // Invitation nominative : seul le téléphone invité peut l'utiliser (lien transféré refusé).
        if (invitation.getTelephone() != null && !invitation.getTelephone().equals(membre.getTelephone())) {
            throw new AccesRefuseException();
        }
        if (participationRepository.existsByMembreIdAndTontineId(membre.getId(), tontine.getId())) {
            throw new ParticipationDejaExistanteException();
        }

        // Mêmes règles que ParticipationService.createParticipation (doublon assumé : 2 usages,
        // règle de trois). Lien de groupe : 1 part, la gestionnaire ajuste ensuite.
        Participation participation = new Participation();
        participation.setTontine(tontine);
        participation.setMembre(membre);
        participation.setNombreParts(invitation.getNombreParts() != null ? invitation.getNombreParts() : 1);
        participation.setStatut(StatutParticipation.ACTIF);
        participation.setDateAdhesion(Date.valueOf(LocalDate.now()));
        participation.setOrdreInscription(participationRepository
                .findFirstByTontineIdOrderByOrdreInscriptionDesc(tontine.getId())
                .map(derniere -> derniere.getOrdreInscription() + 1)
                .orElse(1));
        participationRepository.save(participation);
        // Même message de bienvenue que l'inscription par la gestionnaire.
        notificationService.notifierBienvenue(participation);

        // Usage unique pour l'individuelle ; le lien de groupe reste valable pour les autres.
        if (invitation.getType() == TypeInvitation.INDIVIDUELLE) {
            invitation.setStatut(StatutInvitation.ACCEPTE);
            invitationRepository.save(invitation);
        }
    }

    // 32 octets aléatoires (256 bits) encodés en Base64 URL : impossible à deviner,
    // et utilisable tel quel dans un lien (pas de + / =). Résultat : 43 caractères.
    private String genererToken() {
        byte[] octets = new byte[32];
        hasard.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private InvitationDTO convertiInvitationDTO(Invitation invitation) {
        InvitationDTO dto = new InvitationDTO();
        dto.setId(invitation.getId());
        dto.setTontineId(invitation.getTontine().getId());
        dto.setToken(invitation.getToken());
        dto.setTelephone(invitation.getTelephone());
        dto.setEmail(invitation.getEmail());
        dto.setPrenomPreRempli(invitation.getPrenomPreRempli());
        dto.setNomPreRempli(invitation.getNomPreRempli());
        dto.setNombreParts(invitation.getNombreParts());
        dto.setExpireAt(invitation.getExpireAt());
        dto.setStatut(invitation.getStatut());
        dto.setType(invitation.getType());
        dto.setCreatedAt(invitation.getCreatedAt());

        return dto;
    }
}
