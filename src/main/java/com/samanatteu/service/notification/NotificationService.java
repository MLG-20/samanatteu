package com.samanatteu.service.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.notification.NotificationDTO;
import com.samanatteu.entity.Notification;
import com.samanatteu.enums.StatutNotification;
import com.samanatteu.exception.notification.EnvoiNotificationEchoueException;
import com.samanatteu.repository.NotificationRepository;

@Service 
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDTO> listNotification() {
        return notificationRepository.findAll()
                .stream()
                .map(this::convertiNotificationDTO)
                .toList();
    }

    public NotificationDTO createNotification(Notification notification) {
        if (notification.getStatut() == StatutNotification.ECHEC) {
            throw new EnvoiNotificationEchoueException(notification.getDestinataire().getId());
        }
        Notification enregistree = notificationRepository.save(notification);
        return convertiNotificationDTO(enregistree);
    }

    public Optional<NotificationDTO> updateNotification(Long id, Notification notificationModifier) {
        return notificationRepository.findById(id).map(notificationExsitante -> {
            notificationExsitante.setDestinataire(notificationModifier.getDestinataire());
            notificationExsitante.setTitre(notificationModifier.getTitre());
            notificationExsitante.setMessage(notificationModifier.getMessage());
            notificationExsitante.setType(notificationModifier.getType());
            notificationExsitante.setStatut(notificationModifier.getStatut());
            notificationExsitante.setDateEnvoi(notificationModifier.getDateEnvoi());
            notificationExsitante.setCreatedAt(notificationModifier.getCreatedAt());

            Notification enregistre = notificationRepository.save(notificationExsitante);
            return convertiNotificationDTO(enregistre);
        });
    }

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
