package com.samanatteu.entity.notification;

import java.time.LocalDateTime;

import com.samanatteu.entity.commun.Auditable;
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
import lombok.Setter;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public Notification() {
        super();
    }
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

}
