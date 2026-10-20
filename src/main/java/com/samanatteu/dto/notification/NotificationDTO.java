package com.samanatteu.dto.notification;

import java.time.LocalDateTime;

import com.samanatteu.enums.notification.CanalNotification;
import com.samanatteu.enums.notification.StatutNotification;
import com.samanatteu.enums.notification.TypeNotification;

import lombok.Getter;
import lombok.Setter;

// destinataireId (Long) au lieu de Utilisateur : on n'expose jamais une entité complète imbriquée,
// juste son id (sinon motDePasse fuiterait ici aussi).
@Getter
@Setter
public class NotificationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public NotificationDTO() {
        super();
    }

    private Long id;

    private Long destinataireId;

    private String titre;

    private String message;

    private TypeNotification type;

    private CanalNotification canal;

    private StatutNotification statut;

    private LocalDateTime dateEnvoi;

    private LocalDateTime createdAt;
}
