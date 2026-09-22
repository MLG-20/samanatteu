package com.samanatteu.service.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.notification.NotificationDTO;
import com.samanatteu.entity.Notification;
import com.samanatteu.enums.StatutNotification;
import com.samanatteu.exception.EnvoiNotificationEchoueException;
import com.samanatteu.repository.NotificationRepository;

@Service 
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Lister
    public List<NotificationDTO> listNotification() {
        return notificationRepository.findAll() // 1. List<Notification> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiNotificationDTO) // 3. applique la conversion à CHAQUE Notification -> NotificationDTO
                                                  // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<NotificationDTO> à partir du flux
    }

    // créer
    public NotificationDTO createNotification(Notification notification) {
        if (notification.getStatut() == StatutNotification.ECHEC) {
            throw new EnvoiNotificationEchoueException(notification.getDestinataire().getId());
        }
        Notification enregistree = notificationRepository.save(notification);
        return convertiNotificationDTO(enregistree);
    }

    // update
    public Optional<NotificationDTO> updateNotification(Long id, Notification notificationModifier) {
        // findById(id) renvoie un Optional<Notification> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return notificationRepository.findById(id).map(notificationExsitante -> {
            // notificationExsitante = l'entité déjà en base (trouvée par findById).
            // notificationModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            notificationExsitante.setDestinataire(notificationModifier.getDestinataire());
            notificationExsitante.setTitre(notificationModifier.getTitre());
            notificationExsitante.setMessage(notificationModifier.getMessage());
            notificationExsitante.setType(notificationModifier.getType());
            notificationExsitante.setStatut(notificationModifier.getStatut());
            notificationExsitante.setDateEnvoi(notificationModifier.getDateEnvoi());
            notificationExsitante.setCreatedAt(notificationModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Notification à jour
            // (avec motDePasse).
            Notification enregistre = notificationRepository.save(notificationExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<NotificationDTO>.
            return convertiNotificationDTO(enregistre);
        });
    }

    // Delete
    public boolean deleteNotification(Long id) {
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private NotificationDTO convertiNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setDestinataireId(notification.getDestinataire().getId());
        dto.setTitre(notification.getTitre());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setStatut(notification.getStatut());
        dto.setDateEnvoi(notification.getDateEnvoi());
        dto.setCreatedAt(notification.getCreatedAt());

        return dto;
    }
}
