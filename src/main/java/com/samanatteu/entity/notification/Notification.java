package com.samanatteu.entity.notification;

import java.time.LocalDateTime;

import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.notification.CanalNotification;
import com.samanatteu.enums.notification.StatutNotification;
import com.samanatteu.enums.notification.TypeNotification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private Utilisateur destinataire;

    @Column(name = "titre")
    private String titre;

    @Column(name = "message")
    private String message;

    // Enum depuis V4 (était un String libre).
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TypeNotification type;

    // Une ligne par canal : un SMS et un email pour le même événement = 2 lignes.
    @Enumerated(EnumType.STRING)
    @Column(name = "canal")
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutNotification statut;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
