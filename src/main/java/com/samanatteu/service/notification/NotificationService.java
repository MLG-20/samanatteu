package com.samanatteu.service.notification;

import com.samanatteu.security.UtilisateurConnecte;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.samanatteu.dto.notification.NotificationDTO;
import com.samanatteu.entity.notification.Notification;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.notification.CanalNotification;
import com.samanatteu.enums.notification.StatutNotification;
import com.samanatteu.enums.notification.TypeNotification;
import com.samanatteu.repository.notification.NotificationRepository;

@Service
public class NotificationService {

    private final UtilisateurConnecte utilisateurConnecte;

    private final NotificationRepository notificationRepository;

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    // L'interface, pas EnvoyeurSmsConsole : changer de fournisseur ne touche pas ce service.
    private final EnvoyeurSms envoyeurSms;
    private final EnvoyeurEmail envoyeurEmail;

    public NotificationService(NotificationRepository notificationRepository, EnvoyeurSms envoyeurSms,
            EnvoyeurEmail envoyeurEmail, UtilisateurConnecte utilisateurConnecte) {
        this.notificationRepository = notificationRepository;
        this.envoyeurSms = envoyeurSms;
        this.envoyeurEmail = envoyeurEmail;
        this.utilisateurConnecte = utilisateurConnecte;

    }

    // Chacun ne voit que SES notifications (montants, gains : données privées),
    // comme le journal. Le filtre du service suffit : pas de règle SecurityConfig.
    public List<NotificationDTO> listNotification() {
        return notificationRepository
                .findByDestinataireTelephoneOrderByCreatedAtDesc(utilisateurConnecte.telephone())
                .stream()
                .map(this::convertiNotificationDTO)
                .toList();
    }

    // Seul point d'envoi : tous les services passent par ici. Chaque envoi laisse
    // une ligne (preuve en cas de litige, ECHEC relançables plus tard).
    // SMS toujours, email en plus s'il existe ; isBlank écarte les emails "".
    public void notifier(Utilisateur destinataire, TypeNotification type, String message) {
        envoyer(destinataire, type, CanalNotification.SMS, message);
        if (destinataire.getEmail() != null && !destinataire.getEmail().isBlank()) {
            envoyer(destinataire, type, CanalNotification.EMAIL, message);
        }
    }

    // Un seul bloc pour les deux canaux (pas de try/catch recopié). Un try par
    // canal : un SMS raté n'empêche pas l'email, chacun a sa ligne et son statut.
    private void envoyer(Utilisateur destinataire, TypeNotification type, CanalNotification canal,
            String message) {
        Notification notification = new Notification();
        notification.setDestinataire(destinataire);
        notification.setType(type);
        notification.setCanal(canal);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        if (canal == CanalNotification.EMAIL) {
            notification.setTitre(sujet(type));
        }
        // Un envoi raté ne doit jamais annuler l'action métier (paiement, tirage…) :
        // l'erreur est attrapée ici au lieu de remonter et de déclencher un rollback.
        try {
            if (canal == CanalNotification.SMS) {
                envoyeurSms.envoyer(destinataire.getTelephone(), message);
            } else {
                envoyeurEmail.envoyer(destinataire.getEmail(), notification.getTitre(), message);
            }
            notification.setStatut(StatutNotification.ENVOYE);
            notification.setDateEnvoi(LocalDateTime.now());
        } catch (RuntimeException e) {
            notification.setStatut(StatutNotification.ECHEC);
            logger.warn("Échec de l'envoi {} à l'utilisateur {}", canal, destinataire.getId(), e);
        }
        notificationRepository.save(notification);
    }

    // switch sans default : un nouveau TypeNotification ne compile pas tant
    // qu'on ne lui a pas donné un objet d'email.
    private String sujet(TypeNotification type) {
        return switch (type) {
            case BIENVENUE -> "Bienvenue dans votre tontine";
            case INVITATION -> "Invitation à rejoindre une tontine";
            case PAIEMENT_CONFIRME -> "Paiement reçu";
            case RESULTAT_TIRAGE -> "Résultat du tirage";
            case RAPPEL_COTISATION -> "Rappel de cotisation";
            case RAPPEL_ECHEANCE -> "Rappel d'échéance de prêt";
        };
    }

    // Message écrit une seule fois : utilisé par l'inscription par la gestionnaire
    // ET par l'arrivée via un lien d'invitation.
    public void notifierBienvenue(Participation participation) {
        Utilisateur membre = participation.getMembre();
        Tontine tontine = participation.getTontine();
        notifier(membre, TypeNotification.BIENVENUE,
                tontine.getNom() + " : bienvenue " + membre.getPrenom() + " " + membre.getNom()
                        + " ! Vous avez " + participation.getNombreParts() + " part(s) de "
                        + tontine.getMontantPart().stripTrailingZeros().toPlainString() + " F.");

    }

    private NotificationDTO convertiNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setDestinataireId(notification.getDestinataire().getId());
        dto.setTitre(notification.getTitre());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setCanal(notification.getCanal());
        dto.setStatut(notification.getStatut());
        dto.setDateEnvoi(notification.getDateEnvoi());
        dto.setCreatedAt(notification.getCreatedAt());

        return dto;
    }
}
