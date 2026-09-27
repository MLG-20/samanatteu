package com.samanatteu.dto.notification;

import java.time.LocalDateTime;

import com.samanatteu.enums.StatutNotification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// destinataireId (Long) au lieu de Utilisateur : on n'expose jamais une entité complète imbriquée,
// juste son id (sinon motDePasse fuiterait ici aussi).
@Getter
@Setter
@NoArgsConstructor
public class NotificationDTO {
    private Long id;

    private Long destinataireId;

    private String titre;

    private String message;

    private String type;

    private StatutNotification statut;

    private LocalDateTime dateEnvoi;

    private LocalDateTime createdAt;
}
